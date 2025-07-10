package com.xm.sanvanfo.trandiscore;

import com.xm.sanvanfo.trandiscore.transaction.Transaction;
import lombok.Data;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

@Data
public class TransactionFuncInterceptor {

    private Transaction branchTransaction;
    private TransactionMethodInfo transactionFunc;
    private Method prepareFunc;
    private Method doAfterFunc;
    private Method undoFunc;
    private Object beforeImage;
    private Object afterImage;
    private String xid;
    private Map<String, GlobalLockKey> lockKeys;
    private List<Object> undoLogs;
    private Boolean modified = true;
    private Integer sectionNumber;
    private Object returnObj;
    private String spaceName;
    private Consumer<Object> futureEnd;


    public void prepare() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if(null == prepareFunc) {
            return;
        }
        prepareFunc.setAccessible(true);
        prepareFunc.invoke(getDecorator(), this);
    }

    public Object Do() throws IllegalAccessException, InvocationTargetException, InterruptedException, ExecutionException {

        transactionFunc.getMethodInfo().setAccessible(true);
        Object o =  transactionFunc.getMethodInfo().invoke(getTarget(), null == transactionFunc.getSnapshotArgs() ? transactionFunc.getArgs() :
                transactionFunc.getSnapshotArgs());
        returnObj = o;
        if(!Future.class.isAssignableFrom(o.getClass())) {
            undoLog();
            doAfter();
            return o;
        }
        else {
            if(!CompletionStage.class.isAssignableFrom(o.getClass())) {
                throw new BusinessException("return value must CompletionStage");
            }
            Object undoLog = undoLog();

            if(null != undoLog && (!CompletionStage.class.isAssignableFrom(undoLog.getClass()) ||
                    !Future.class.isAssignableFrom(undoLog.getClass()))) {
                throw new BusinessException("undoLog must implements Future<Boolean>, CompletionStage<Boolean>");
            }
            Map<String, CompletionStage> executions = new HashMap<>();

            executions.put("return", (CompletionStage)o);
            if(null != undoLog) {
                executions.put("undoLog", (CompletionStage) undoLog);
            }
            try {
                return createConvertFuture(executions, v -> {
                    try {
                        Future undoLogExecution = (Future) executions.get("undoLog");
                        Future ret = (Future) executions.get("return");
                        returnObj = ret.get();
                        if(undoLogExecution != null) {
                            undoLogExecution.get();
                        }
                        Object doAfterExecution = doAfter();
                        if(doAfterExecution != null) {
                            if(Future.class.isAssignableFrom(doAfterExecution.getClass())) {
                                ((Future) doAfterExecution).get();
                            }
                        }
                        return returnObj;
                    }
                    catch (Exception ex) {
                        throw new BusinessException(ex, "TransactionFuncInterceptor Do get() error");
                    }
                    finally {
                        if(null != futureEnd) {
                            futureEnd.accept(returnObj);
                        }
                    }
                });
            }
            catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private Object getDecorator() {
        return transactionFunc.getDecorator();
    }

    private Object getTarget() {
        return transactionFunc.getTarget();
    }

    private Object doAfter() throws  IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if(null == doAfterFunc) {
            return null;
        }
        doAfterFunc.setAccessible(true);
        return  doAfterFunc.invoke(getDecorator(), this);
    }

    private Object undoLog() throws  IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if(null == undoFunc) {
            return null;
        }
        undoFunc.setAccessible(true);
        return undoFunc.invoke(getDecorator(), this);
    }

    private Object createConvertFuture(Map<String, CompletionStage> executions, Function o) throws Throwable {
        Object decorator = getDecorator();
        Method method = decorator.getClass().getDeclaredMethod("createConvertFuture", Map.class, Function.class);
        method.setAccessible(true);
        return method.invoke(decorator, executions, o);
    }
}

package com.xm.sanvanfo.trandiscore.proxy;


import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.common.utils.ClassUtils;
import com.xm.sanvanfo.common.utils.CommonUtils;
import com.xm.sanvanfo.trandiscore.*;
import com.xm.sanvanfo.trandiscore.annotation.TransactionFunc;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;
import com.xm.sanvanfo.trandiscore.transaction.TransactionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;

@SuppressWarnings({"unused"})
@Slf4j
public class ProxyFactory {

    private static ProxyFactory factory = new ProxyFactory();

    public static ProxyFactory INSTANCE()  {
        return factory;
    }

    public Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private InvocationHandler handler;
        private ClassLoader classLoader;

        @SuppressWarnings("unchecked")
        public synchronized Object proxy(Class<?> clazz, Object o, TransactionLevelEx defaultLevel) {
            if(null == classLoader) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
            if(null == handler) {
                handler = DefaultHandler.create(o, defaultLevel);
            }
			else {
				((DefaultHandler)handler).setObj(o);
				((DefaultHandler)handler).setLevelEx(defaultLevel);
			}
            return Proxy.newProxyInstance(classLoader, ClassUtils.getAllInterfaces(clazz).toArray(new Class[]{}), handler);
        }

        public Builder handler(InvocationHandler h) {
            this.handler = h;
            return this;
        }

        public Builder classLoader(ClassLoader loader) {
            this.classLoader = loader;
            return this;
        }

    }

    //default handler dealing with @TransactionFunc
	@Data
    public static class DefaultHandler implements InvocationHandler {

        private Object obj;
        private TransactionLevelEx levelEx;

        DefaultHandler(Object o, TransactionLevelEx levelEx) {
            this.obj = o;
            this.levelEx = levelEx;
        }
         static InvocationHandler create(Object o, TransactionLevelEx levelEx) {
            return new DefaultHandler(o, levelEx);
        }

        //todo:这里还有事后需要再Lock的情况，比如自增主键的问题
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method classMethod = obj.getClass().getMethod(method.getName(), method.getParameterTypes());
            if(!TransactionManager.INSTANCE().isTransactionActive() && !TrandisLockManager.isEnableLock()) {
                return method.invoke(obj, args);
            }
            TransactionFunc func = classMethod.getAnnotation(TransactionFunc.class);
            if(null == func) {
                return method.invoke(obj, args);
            }
            List<GlobalLockKey> list = getLocks(func, obj, method.getParameterTypes(), args);
            List<String> zkLocks = null == list ? null : list.stream().map(GlobalLockKey::getKeyPath).collect(Collectors.toList());
            List<GlobalLockKey> readLocks = getWriteMethodReadList(func, obj, method.getParameterTypes(), args);
            List<String> readKeys = null == readLocks ? null :
                    readLocks.stream().map(GlobalLockKey::getKeyPath).collect(Collectors.toList());
            List<String> read = func.write() || func.lockUpgrade() ? readKeys : zkLocks;
            List<String> write = func.write() || func.lockUpgrade() ? zkLocks : null;
            Object decorator = obj;
            if(IProxyDecorator.class.isAssignableFrom(obj.getClass())) {
                decorator = ((IProxyDecorator)proxy).getDecorator();
            }
            if(TransactionManager.INSTANCE().isTransactionActive()) {
                TransactionFuncInterceptor interceptor = new TransactionFuncInterceptor();
                TransactionInfo tInfo = TransactionManager.INSTANCE().getCurrentTransactionInfo();
                interceptor.setBranchTransaction(tInfo.getBranchTransaction());
                interceptor.setSpaceName(getSpaceName(func, decorator));
                if(StringUtils.isNotEmpty(func.undoFunction())) {
                    interceptor.setUndoFunc(decorator.getClass().getDeclaredMethod(func.undoFunction(), TransactionFuncInterceptor.class));
                }
                if(StringUtils.isNotEmpty(func.prepareFunction())) {
                    interceptor.setPrepareFunc(decorator.getClass().getDeclaredMethod(func.prepareFunction(), TransactionFuncInterceptor.class));
                }
                if(StringUtils.isNotEmpty(func.doAfterFunction())) {
                    interceptor.setDoAfterFunc(decorator.getClass().getDeclaredMethod(func.doAfterFunction(), TransactionFuncInterceptor.class));
                }
                interceptor.setXid(tInfo.getXid());
                TransactionMethodInfo methodInfo = new TransactionMethodInfo();
                methodInfo.setArgs(args);
                methodInfo.setMethodInfo(classMethod);
                methodInfo.setTarget(obj);
                methodInfo.setDecorator(decorator);
                interceptor.setTransactionFunc(methodInfo);
                interceptor.setSectionNumber(tInfo.getCurrentSection());
                interceptor.setLockKeys(new HashMap<>());
                Optional.ofNullable(readLocks).ifPresent(r -> r.forEach(o-> {
                    o.setXid(tInfo.getXid());
                    o.setWrite(false);
                    o.setLockUpgrade(func.lockUpgrade());
                    o.setSectionNumber(tInfo.getCurrentSection());
                    interceptor.getLockKeys().putIfAbsent(String.format("%s-%s-%s-read", o.getSpaceName(), o.getKeyId(), o.getSubKeyId()), o);
                }));
                Asserts.noNull(list);
                list.forEach(o->{
                    o.setXid(tInfo.getXid());
                    o.setWrite(func.write());
                    o.setLockUpgrade(func.lockUpgrade());
                    o.setSectionNumber(tInfo.getCurrentSection());
                    interceptor.getLockKeys().putIfAbsent(String.format("%s-%s-%s-%s", o.getSpaceName(), o.getKeyId(), o.getSubKeyId(),
                            func.write() || func.lockUpgrade() ? "write" : "read"), o);
                });
                tInfo.getBranchTransaction().addInterceptor(interceptor);

                Long begin = System.currentTimeMillis();
                String lockId = CommonUtils.uuid();
                boolean futureResult = false;
                try {
                    transactionLockResource(lockId, tInfo.getXid(), read, write, tInfo.getTimeout());
                    Object result =  invokeMethod(interceptor,o->{
                        transactionReleaseReadResource(lockId, tInfo.getXid(), read, write, tInfo.getTimeout());
                        Long end = System.currentTimeMillis();
                        log.debug(String.format("--------- %dms class:%s,method:%s",
                                end - begin, obj.getClass().getCanonicalName(), method.getName()));
                    });
                    futureResult = null != result && Future.class.isAssignableFrom(result.getClass());
                    return result;
                }
                catch (Exception ex) {
                    Throwable throwable = ex;
                   while (throwable instanceof InvocationTargetException) {
                       throwable = ((InvocationTargetException) ex).getTargetException();
                   }
                   if(throwable instanceof TransactionException) {
                       throw throwable;
                   }
                    throw new BusinessException(throwable, throwable.getMessage());
                }
                finally {
                    if(!futureResult) {
                        transactionReleaseReadResource(lockId, tInfo.getXid(), read, write, tInfo.getTimeout());
                        Long end = System.currentTimeMillis();
                        log.debug(String.format("--------- %dms class:%s,method:%s",
                                end - begin, obj.getClass().getCanonicalName(), method.getName()));
                    }
                }

            }
            else {
                boolean resultFuture = false;
                try {

                    //locking directly

                    acquire(levelEx, read, write, GlobalLockManager.INSTANCE().config().getDefaultTimeout());
                    Object o = method.invoke(obj, args);
                    if(o == null || !Future.class.isAssignableFrom(o.getClass())) {
                        return o;
                    }
                    else {
                        resultFuture = true;
                        Method convertMethod = decorator.getClass().getDeclaredMethod("createConvertFuture", Map.class, Function.class);
                        convertMethod.setAccessible(true);
                        Map<String, CompletionStage> executions = new HashMap<>();
                        executions.put("return", (CompletionStage)o);
                        Function function = v->{
                            try {
                                Future future = (Future) executions.get("return");
                                return future.get();
                            }
                            catch (Exception ex) {
                                throw new BusinessException(ex, "No Transaction future get error");
                            }
                            finally {
                                try {
                                    release(levelEx, read, write);
                                }
                                catch (Throwable ex) {
                                    log.error("release lock error:{}", ex.toString());
                                }
                            }
                        };
                        return convertMethod.invoke(decorator, executions, function);

                    }
                }
                catch (Exception ex) {
                    log.warn(BusinessException.exceptionFullMessage(ex));
                    throw ex;
                }
               finally {
                    if(!resultFuture) {
                        release(levelEx, read, write);
                    }
                }
            }
        }

        private void transactionLockResource(String lockId,  String xid, List<String> read, List<String> write, Long timeout) {
            Long begin = System.currentTimeMillis();
            TransactionManager.INSTANCE().lockResource(lockId, xid, read, write, timeout);
            Long end = System.currentTimeMillis();
            log.debug(String.format("-----------%sms xid:%s lock read lock:%s write lock:%s", end - begin, xid,
                   read == null ? "" : String.join(",", read),
                    write == null ? "" : String.join(",", write)));
        }

        private void transactionReleaseReadResource(String lockId, String xid, List<String> read, List<String> write, Long timeout) {
            if(null == read || 0 == read.size()) {
                return;
            }
            Long begin = System.currentTimeMillis();
            TransactionManager.INSTANCE().releaseReadResource(lockId, xid, read, write, timeout);
            Long end = System.currentTimeMillis();
            log.debug(String.format("-----------%dms xid：%s release read lock:%s", end - begin, xid,
                    String.join(",", read)));

        }

        private String getSpaceName(TransactionFunc func, Object decorator) throws Throwable {
            Method method = decorator.getClass().getDeclaredMethod(func.spaceName());
            method.setAccessible(true);
            Object obj = method.invoke(decorator);
            return (String)obj;
        }

        @SuppressWarnings("WeakerAccess")
        protected Object invokeMethod(TransactionFuncInterceptor interceptor, Consumer<Object> futureEnd) throws Throwable {
            interceptor.setFutureEnd(futureEnd);
            interceptor.prepare();
            return interceptor.Do();
        }

        private Object createConvertFuture(Object proxy, Map<String, CompletionStage> executions, Function o) throws Throwable {
            if(IProxyDecorator.class.isAssignableFrom(proxy.getClass())) {
                proxy = ((IProxyDecorator)proxy).getDecorator();
            }
            Method method = proxy.getClass().getDeclaredMethod("createConvertFuture", Map.class, Function.class);
            method.setAccessible(true);
            return method.invoke(proxy, executions, o);
        }

        private List<GlobalLockKey> getLocks(TransactionFunc func, Object proxy, Class[] classes, Object[] args) throws Throwable{
            String zookeeperLocks = func.zookeeperLockKeysFunction();
           return getLockListInner(zookeeperLocks, proxy, classes, args);
        }


        private List<GlobalLockKey> getWriteMethodReadList(TransactionFunc func, Object proxy, Class[] classes, Object[] args) throws Throwable {
            String zookeeperLocks = func.zookeeperReadLockKeysFunction();
            return getLockListInner(zookeeperLocks, proxy, classes, args);
        }

        @SuppressWarnings({"unchecked"})
        private List<GlobalLockKey> getLockListInner(String locks, Object proxy, Class[] classes, Object[] args) throws Exception {
            if(StringUtils.isEmpty(locks)) {
                return null;
            }
            if(IProxyDecorator.class.isAssignableFrom(proxy.getClass())) {
                proxy = ((IProxyDecorator)proxy).getDecorator();
            }
            Method m = proxy.getClass().getDeclaredMethod(locks, classes);
            m.setAccessible(true);
            return  (List)m.invoke(proxy, args);
        }

        private void acquire(TransactionLevelEx levelEx, List<String> read, List<String> write, Long timeout) throws Throwable {
            GlobalLockManager.INSTANCE().acquire(levelEx, read, timeout, TimeUnit.SECONDS, write, timeout, TimeUnit.SECONDS);
        }

        private void release(TransactionLevelEx levelEx, List<String> read, List<String> write) throws Throwable {
            GlobalLockManager.INSTANCE().release(levelEx, read,write);
        }


    }
}

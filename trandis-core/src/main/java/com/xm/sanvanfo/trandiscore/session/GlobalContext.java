package com.xm.sanvanfo.trandiscore.session;

import com.xm.sanvanfo.common.Disposable;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.constant.TransactionLevelEx;
import com.xm.sanvanfo.trandiscore.netty.AbstractNettyRemotingServer;
import com.xm.sanvanfo.trandiscore.netty.processor.server.BranchRollbackProcessor;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.xm.sanvanfo.trandiscore.protocol.MessageTypeAware.MessageType.TYPE_BRANCH_ROLLBACK_MSG_REQUEST;

@Slf4j
public class GlobalContext implements Disposable {

    private static final GlobalContext context = new GlobalContext();

    private HashedWheelTimer timer = new HashedWheelTimer();
    private SessionStore sessionStore;
    private BranchRollbackProcessor processor;
    private AbstractNettyRemotingServer server;
    private final ConcurrentHashMap<String, GlobalSession> sessionMap = new ConcurrentHashMap<>();
    private  TransactionLevelEx levelEx;
    private ExecutorService executorService;

    public static GlobalContext INSTANCE() {
        return context;
    }

    public void initSessionStore(SessionStore sessionStore, int threadNum) {
        this.sessionStore = sessionStore;
        executorService = new ThreadPoolExecutor(threadNum, 1024, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1024));
    }

    public void transactionLevel(TransactionLevelEx levelEx) {
        this.levelEx = levelEx;
    }

    public void setServer(AbstractNettyRemotingServer server) {
        this.processor = (BranchRollbackProcessor) server.getProcessor(TYPE_BRANCH_ROLLBACK_MSG_REQUEST.getCode());
        this.server = server;
    }

    public void createSession(GlobalTransactionInfo transactionInfo) {

        String xid = transactionInfo.getXid();
        GlobalSession session = new GlobalSession(transactionInfo, sessionStore, executorService,
                levelEx, server);
        sessionMap.put(xid, session);
        session.initLock();
        long second = transactionInfo.getTimeout();
        timer.newTimeout(new SessionTimerTask(session, this::sessionTimeout), second, TimeUnit.SECONDS);
    }

    public void closeSession(String xid) {
        GlobalSession session = sessionMap.remove(xid);
        session.clean();
        if(sessionStore.sync()) {
            sessionStore.delete(xid);
        }
        else {
            executorService.execute(()-> sessionStore.delete(xid));
        }
    }

    public GlobalSession getSession(String xid) {
         if(sessionMap.containsKey(xid)) {
             return sessionMap.get(xid);
         }
         synchronized (this) {
             if(sessionMap.containsKey(xid)) {
                 return sessionMap.get(xid);
             }
             GlobalSession session = sessionStore.load(xid);
             if (null == session) {
                 return null;
             }
             sessionMap.put(xid, session);
             session.initLock();
             long second = session.getTransactionInfo().getTimeout();
             timer.newTimeout(new SessionTimerTask(session, this::sessionTimeout), second, TimeUnit.SECONDS);
             return session;
         }
    }

    private void sessionTimeout(GlobalSession session) {
        executorService.execute(()-> {
            String xid = session.getTransactionInfo().getXid();
            if (sessionMap.containsKey(xid)) {
                GlobalSession storeSession = sessionStore.load(xid);
                if(null == storeSession || !storeSession.equals(session)) {
                    sessionMap.remove(xid);
                    session.clean();
                    return;
                }
                try {
                    processor.sendSyncBranchRollback(session);
                    log.warn(String.format("session:%s timeout, transaction name is %s roll backed", xid,
                            session.getTransactionInfo().getTransactionName()));
                } catch (Exception ex) {
                    log.error(String.format("session:%s timeout, transaction name is %s send rollback error:%s", xid,
                            session.getTransactionInfo().getTransactionName(), BusinessException.exceptionFullMessage(ex)));
                }
                closeSession(xid);
            }
        });
    }

    @Override
    public void dispose() {
        timer.stop();
    }

    private static class SessionTimerTask implements TimerTask {

        private final Consumer<GlobalSession> consumer;
        private final GlobalSession globalSession;

        private SessionTimerTask(GlobalSession globalSession, Consumer<GlobalSession> consumer) {
            this.globalSession = globalSession;
            this.consumer = consumer;
        }

        @Override
        public void run(Timeout timeout)  {
            consumer.accept(globalSession);
        }
    }
}

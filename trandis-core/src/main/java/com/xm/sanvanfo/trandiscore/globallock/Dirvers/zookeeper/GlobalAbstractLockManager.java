package com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper;

import com.xm.sanvanfo.common.utils.LruThreadSafeGetter;
import com.xm.sanvanfo.trandiscore.BusinessException;
import com.xm.sanvanfo.trandiscore.netty.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public abstract class GlobalAbstractLockManager<T> implements Disposable {

    final CuratorFramework curatorFramework;
    protected final String namespace;
    private final String workspace;

    private final LruThreadSafeGetter<String, T> locks = new LruThreadSafeGetter<>(3600*24L, 30L, o-> {
        String wrapPath = wrapPath(o);
        return createInterProcessLock(wrapPath);
    });
    private ExecutorService threadPoolExecutor;


    GlobalAbstractLockManager(CuratorFramework framework, String namespace, String workspace) {
        curatorFramework = framework;
        this.namespace = namespace;
        this.workspace = workspace;
        threadPoolExecutor = Executors.newFixedThreadPool(5);
    }

    @Override
    public void dispose() {
        locks.dispose();
    }

    T getSyncInterProcessLock(String path) {
        return locks.get(path);
    }

    private String wrapPath(String path) {
        return String.format("/%s/%s/%s", namespace, workspace, path);
    }

    protected abstract T createInterProcessLock(String wrapPath);

    void checkDeleteNode(String path) {
        if(ZookeeperLockDriver.hasContainerSupport) {
            return;
        }
        //it is deleted, if another thread get it, it will trigger a callback no exist exception to rebuild
        String wrapPath = wrapPath(path);
        threadPoolExecutor.execute(() ->{
            try {
                List<String> children = curatorFramework.getChildren().forPath(wrapPath);
                if (null == children || 0 == children.size()) {
                    try {
                        curatorFramework.delete().forPath(wrapPath);
                    } catch (KeeperException.NotEmptyException | KeeperException.NoNodeException ignore) {

                    } catch (Exception ex) {
                        log.warn("delete node error:" + BusinessException.exceptionFullMessage(ex));
                    }
                }
            }
            catch (KeeperException.NoNodeException ignore){}
            catch(Exception exx) {
                throw new BusinessException(exx, exx.getMessage());
            }
        });
    }
}

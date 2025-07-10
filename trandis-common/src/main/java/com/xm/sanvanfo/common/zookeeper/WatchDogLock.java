package com.xm.sanvanfo.common.zookeeper;

import com.xm.sanvanfo.common.Constants;
import com.xm.sanvanfo.common.utils.CommonUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WatchDogLock {

    private final CuratorFramework curatorFramework;
    private final String path;
    private final String token;
    private final String threadId;
    private String nodePath;
    private String parentNode;

    public WatchDogLock(CuratorFramework curatorFramework, String path, String token) {

        this.curatorFramework = curatorFramework;
        this.path = path;
        int pos = path.lastIndexOf(Constants.ENDPOINT_BEGIN_CHAR);
        parentNode = path.substring(0, pos);
        this.token = token;
        threadId = CommonUtils.uuid();
    }

    public void acquire() throws Exception {
       acquire(null, null);
    }

    public boolean acquire(Long time, TimeUnit unit) throws Exception {
        long startMills = System.currentTimeMillis();
        nodePath = createNode();
        while (true) {
            try {
                List<String> children = getDataAndWatch();
                boolean ret = (null == children || children.size() == 0) ||
                        findFirstChildContainsToken(parentNode + Constants.ENDPOINT_BEGIN_CHAR  + children.get(0));
                if (ret) {
                    return true;
                }
                synchronized (this) {
                    if (null != time) {
                        long waitMills = unit.toMillis(time);
                        if (System.currentTimeMillis() - startMills > waitMills) {
                            return false;
                        }
                        wait(waitMills);
                    } else {
                        wait();
                    }
                }
            }
            catch (Exception ex) {
                try {
                    release();
                }
                catch (Exception ignore) {
                }
                throw ex;
            }
        }
    }

    private boolean findFirstChildContainsToken(String s) throws Exception {

        String data = new String(getData(s));
        return data.contains(token + "-");
    }


    public void release() throws Exception {
        deleteNode(nodePath);
    }

    private String createNode() throws Exception {
        return curatorFramework.create().creatingParentsIfNeeded().
                withMode(CreateMode.EPHEMERAL_SEQUENTIAL).withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE).forPath(path,
                String.format("%s-%s", token, threadId).getBytes());
    }

    private byte[] getData(String ourPath) throws Exception {
        Stat stat=new Stat();
        return curatorFramework.getData().storingStatIn(stat).forPath(ourPath);
    }

    private void deleteNode(String ourPath) throws Exception {
        curatorFramework.delete().guaranteed().forPath(ourPath);
    }

    private List<String> getDataAndWatch()  throws Exception {
        try {

            return  curatorFramework.getChildren().usingWatcher((CuratorWatcher) event -> {
                synchronized (this) {
                        notifyAll();
                }
            }).forPath(parentNode);
        }
        catch (KeeperException.NoNodeException ignore) {
            return null;
        }
    }

}

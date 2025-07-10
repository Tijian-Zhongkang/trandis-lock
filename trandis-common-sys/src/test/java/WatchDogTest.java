import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.junit.jupiter.api.Test;
import com.xm.sanvanfo.common.zookeeper.WatchDogLock;

public class WatchDogTest {

    @Test
    public void watchDogTest() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient("192.168.1.201:2181", new RetryOneTime(100));
        client.start();
        String token = "testToken";
        WatchDogLock dog = new WatchDogLock(client,"/watchdog/test", token);
        dog.acquire();
        WatchDogLock dog1 = new WatchDogLock(client, "/watchdog/test", token);
        dog1.acquire();
        dog1.release();
        dog.release();
    }

    @Test
    public void watchDogDifferentTokenTest() throws Exception {
        CuratorFramework client = CuratorFrameworkFactory.newClient("192.168.1.201:2181", new RetryOneTime(100));
        client.start();
        String token = "testToken";
        WatchDogLock dog = new WatchDogLock(client,"/watchdog/test", token);
        dog.acquire();
        token = "testToken2";
        WatchDogLock dog1 = new WatchDogLock(client, "/watchdog/test", token);
        dog1.acquire();
        dog1.release();
        dog.release();
    }

}

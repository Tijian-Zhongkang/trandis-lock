import com.xm.sanvanfo.trandiscore.globallock.Dirvers.zookeeper.ZookeeperLockDriver;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockConfiguration;
import com.xm.sanvanfo.trandiscore.globallock.GlobalLockManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GlobalLockTest {


    @Test
    public void reentrantLockTest() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(()->{
            int threadCount = 0;
            lock.lock();
            threadCount++;
            System.out.println("thread:" + threadCount);
            lock.lock();
            threadCount++;
            System.out.println("thread:" + threadCount);
            lock.unlock();
            lock.unlock();
            latch.countDown();
        });
        int mainCount = 0;
        lock.lock();
        mainCount++;
        System.out.println("main:" + mainCount);
        lock.lock();
        mainCount++;
        System.out.println("main:" + mainCount);
        lock.unlock();
        lock.unlock();
        latch.countDown();
        latch.await();
    }


    @Test
    public void fairThroughputTest() throws Exception {
        throughputTest(true);
    }

    @Test
    public void unfairThroughputTest() throws Exception {
        throughputTest(false);
    }


    private void throughputTest(boolean fair) throws Exception {
        ReentrantLock lock = new ReentrantLock(fair);
        CountDownLatch latch = new CountDownLatch(500);
        long begin = System.currentTimeMillis();
        ExecutorService service = Executors.newFixedThreadPool(50);
        for(int i = 0; i < 500; i++) {
            service.execute(() -> {
                try {
                    lock.lock();
                    Thread.sleep(30);
                    lock.unlock();
                    latch.countDown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            if(i % 50 == 0) {
                Thread.sleep(1000);
            }
        }
        latch.await();
        System.out.println("thread1 lock consumed " + (System.currentTimeMillis() - begin) + "ms");
    }

    @Test
    public void hungryReentrantLockTest() throws Exception {
        ReentrantLock lock = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(()->{
            try {
                Thread.sleep(1000);
                long begin = System.currentTimeMillis();
                lock.lock();
                System.out.println("thread1 lock consumed " + (System.currentTimeMillis() - begin) + "ms");
                Thread.sleep(1000);
                lock.unlock();
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        for(int i = 0; i < 30; i++) {

            try {
                lock.lock();
                Thread.sleep(1000);
                lock.unlock();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        latch.countDown();
        latch.await();
    }

    @Test
    public void reentrantReadWriteLockTestTrue() {
        reentrantReadWriteLockTest(true);
    }

    @Test
    public void reentrantReadWriteLockTestFalse() {
        reentrantReadWriteLockTest(false);
    }

    @Test
    public void hungryReentrantReadWriteLockTest() throws Exception {
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        CountDownLatch latch = new CountDownLatch(31);
        ExecutorService service = Executors.newSingleThreadExecutor();
        ExecutorService readService = Executors.newFixedThreadPool(30);
        service.execute(()->{
            try {
                Thread.sleep(1000);
                long begin = System.currentTimeMillis();
                readWriteLock.writeLock().lock();
                System.out.println("thread1 lock consumed " + (System.currentTimeMillis() - begin) + "ms");
                Thread.sleep(1000);
                readWriteLock.writeLock().unlock();
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        for(int i = 0; i < 30; i++) {

            readService.execute(()->{
                readWriteLock.readLock().lock();
                try {
                    Thread.sleep(2000);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                readWriteLock.readLock().unlock();
                latch.countDown();
            });
            Thread.sleep(1000);
        }
        latch.await();
    }


    private void reentrantReadWriteLockTest(boolean threadSleep) {
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(()->{
            try {
                if (threadSleep) {
                    Thread.sleep(1000);
                }
                readWriteLock.writeLock().lock();
                System.out.println("thread1 acquire write lock");
                Thread.sleep(2000);
                readWriteLock.writeLock().unlock();
                System.out.println("thread1 release write lock");
                Thread.sleep(1000);
                readWriteLock.readLock().lock();
                System.out.println("thread1 acquire read lock");
                Thread.sleep(2000);
                readWriteLock.readLock().unlock();
                System.out.println("thread1 release read lock");
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        try {
            if (!threadSleep) {
                Thread.sleep(1000);
            }
            readWriteLock.readLock().lock();
            System.out.println("main acquire read lock");
            Thread.sleep(2000);
            readWriteLock.readLock().unlock();
            System.out.println("main release read lock");
            Thread.sleep(1000);
            readWriteLock.writeLock().lock();
            System.out.println("main acquire write lock");
            Thread.sleep(2000);
            System.out.println("main release write lock");
            readWriteLock.writeLock().unlock();
            latch.countDown();
            latch.await();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void LocalLockTest() throws Exception {

        List<ReentrantLock> list = new ArrayList<>();
        for(int i = 0; i < 50000; i++) {
            ReentrantLock lock = new ReentrantLock();
            list.add(lock);
        }
        long begin = System.currentTimeMillis();
        for (ReentrantLock lock:list
             ) {
            lock.lock();
        }
        long end = System.currentTimeMillis();
        System.out.println(String.format("--------------acquire:%dms", end - begin ));
        for (ReentrantLock lock:list
        ) {
            lock.unlock();
        }
        long nextEnd = System.currentTimeMillis();
        System.out.println(String.format("--------------release:%dms", nextEnd - end ));
    }

    @Test
    public void zkHungryReentrantReadWriteLockTest() throws Exception {
        ZookeeperLockDriver driver = defaultDriver();
        CuratorFramework curatorFramework = (CuratorFramework) driver.getConfig().getFramework();
        InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(curatorFramework, "/pathtest1");
        CountDownLatch latch = new CountDownLatch(31);
        ExecutorService service = Executors.newSingleThreadExecutor();
        ExecutorService readService = Executors.newFixedThreadPool(30);
        service.execute(()->{
            try {
                Thread.sleep(1000);
                long begin = System.currentTimeMillis();
                readWriteLock.writeLock().acquire();
                System.out.println("thread1 lock consumed " + (System.currentTimeMillis() - begin) + "ms");
                Thread.sleep(1000);
                readWriteLock.writeLock().release();
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        for(int i = 0; i < 30; i++) {

            readService.execute(()->{

                try {
                    readWriteLock.readLock().acquire();
                    Thread.sleep(2000);
                    readWriteLock.readLock().release();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                latch.countDown();
            });
            Thread.sleep(1000);
        }
        latch.await();
    }

    @Test
    public void delNode() throws Exception {
        ZookeeperLockDriver driver = defaultDriver();
        CuratorFramework curatorFramework = (CuratorFramework) driver.getConfig().getFramework();
        for (int i = 0; i < 10000; i++) {
            String path = "/pathtest" + i;
            try {
                curatorFramework.delete().guaranteed().forPath(path);
            }
            catch (Exception ignore){}
        }
    }

    @Test
    public void zkLock100() throws Exception {
        ZookeeperLockDriver driver = defaultDriver();
        CuratorFramework curatorFramework = (CuratorFramework) driver.getConfig().getFramework();
       List<InterProcessMutex> list = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            InterProcessMutex mutex = new InterProcessMutex(curatorFramework, "/pathtest" + i);
            list.add(mutex);
        }
        long begin = System.currentTimeMillis();
        for(InterProcessMutex mutex:list) {
            mutex.acquire();
        }
        long end = System.currentTimeMillis();
        System.out.println("---------------------acquire" + ":" + (end - begin) + "ms");
        for(InterProcessMutex mutex:list) {
            mutex.release();
        }
        long nexEnd = System.currentTimeMillis();
        System.out.println("---------------------release" + ":" + (nexEnd - end) + "ms");
    }

    @Test
    public void MixedLock() throws Exception {
        initGlobalLock();
        ExecutorService service = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(100);
        for(int i = 0; i < 100; i++) {
            final  Integer t = i;
            service.execute(() ->{
                try {
                    Random random = new Random();
                    Integer count = Math.abs(random.nextInt() % 5);
                    if(count.equals(0)) {
                        count = 1;
                    }
                    List<String> path = new ArrayList<>();
                    HashSet<Integer> set = new HashSet<>();
                    for(int j =0; j < count; j++) {
                        Random r = new Random();
                        Integer pathNo = Math.abs(r.nextInt() % 5);
                        while(set.contains(pathNo)) {
                            pathNo = Math.abs(r.nextInt() % 5);
                        }
                        set.add(pathNo);
                        path.add(String.format("test-%d", pathNo));
                    }
                    if(path.size() == 1) {
                        GlobalLockManager.INSTANCE().getSingleLockManager().acquire(path.get(0));
                        System.out.println(String.format("single lock:%s, process:%d", path.get(0), t));
                    }
                    else {
                        GlobalLockManager.INSTANCE().getMultiLockManager().acquire(path);
                        System.out.println(String.format("multi lock:%s, process:%d", String.join(" , ", path), t));
                    }

                    Thread.sleep(10);
                    if(path.size() == 1) {
                        GlobalLockManager.INSTANCE().getSingleLockManager().release(path.get(0));
                        System.out.println(String.format("release single lock:%s, process:%d", path.get(0), t));
                    }
                    else {
                        GlobalLockManager.INSTANCE().getMultiLockManager().release(path);
                        System.out.println(String.format("release multi lock:%s, process:%d", String.join(" , ", path), t));
                    }
                    latch.countDown();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        }
        latch.await();
    }

    @Test
    public void singleLock()  throws Exception {
        initGlobalLock();
        ExecutorService service = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(100);
        Long begin = System.currentTimeMillis();
        for(int i = 0; i < 100; i++) {
            final int t = i;
            service.execute(() -> {
                Random r = new Random();
                int lock = Math.abs(r.nextInt() % 1);
                try {
                    Long b = System.currentTimeMillis();
                    GlobalLockManager.INSTANCE().getSingleLockManager().acquire("test-" + lock);
                    Long e = System.currentTimeMillis();
                    System.out.println(String.format("lock:%d, process:%d, time:%dms", lock, t, (e - b)));
                    Thread.sleep(200);
                    GlobalLockManager.INSTANCE().getSingleLockManager().release("test-" + lock);
                    System.out.println(String.format("release:%d, process:%d", lock, t));
                    latch.countDown();
                }
                catch (Exception ex) {
                   ex.printStackTrace();
                }
            });
        }
        latch.await();
        Long end = System.currentTimeMillis();
        System.out.println((end - begin) + "ms / 1000");
    }

    @Test
    public void multiLock() throws Exception {
        initGlobalLock();
        ExecutorService service = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(100);
        for(Integer i = 0; i < 100; i++) {
            final  Integer t = i;
            service.execute(() ->{
                try {
                    Random random = new Random();
                    Integer count = Math.abs(random.nextInt() % 5);
                    if(count.equals(0)) {
                        count = 1;
                    }
                    List<String> path = new ArrayList<>();
                    HashSet<Integer> set = new HashSet();
                    for(int j =0; j < count; j++) {
                        Random r = new Random();
                        Integer pathNo = Math.abs(r.nextInt() % 5);
                        while(set.contains(pathNo)) {
                            pathNo = Math.abs(r.nextInt() % 5);
                        }
                        set.add(pathNo);
                        path.add(String.format("test-%d", pathNo));
                    }
                    GlobalLockManager.INSTANCE().getMultiLockManager().acquire(path);
                    System.out.println(String.format("lock:%s, process:%d", String.join(" , ", path), t));
                    Thread.sleep(1000);
                    GlobalLockManager.INSTANCE().getMultiLockManager().release(path);
                    latch.countDown();
                    System.out.println(String.format("release lock:%s, process:%d", String.join(" , ", path), t));
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

        }
        latch.await();
    }


    @Test
    public void intMax() {
         final int MASK = 0x7FFFFFFF;
        AtomicInteger integer = new AtomicInteger(Integer.MAX_VALUE);
        int next = integer.incrementAndGet() & MASK;
        System.out.println(next);
    }




    public static void initGlobalLock() {
        GlobalLockManager.init(defaultDriver());
    }

    public static ZookeeperLockDriver defaultDriver() {
        ZookeeperLockDriver driver =  new ZookeeperLockDriver();
        driver.init(defaultLockConfiguration());
        return driver;
    }

    public static GlobalLockConfiguration defaultLockConfiguration() {
         CuratorFramework client = CuratorFrameworkFactory.newClient("192.168.1.201:2181", new ExponentialBackoffRetry(1, 3));
         client.start();
        GlobalLockConfiguration configuration = GlobalLockConfiguration.create("default", client, 5L);
        return configuration;
    }
}

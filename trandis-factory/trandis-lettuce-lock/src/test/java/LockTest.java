
import com.xm.sanvanfo.common.CallBackException;
import com.xm.sanvanfo.lock.TMultiReadWriteLock;
import com.xm.sanvanfo.lock.TReentrantLock;
import com.xm.sanvanfo.lock.TReentrantReadWriteLock;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class LockTest {

    private final boolean run = true;

    @Test
    public void TReentrantLockTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        TReentrantLock lock = new TReentrantLock("path1");
        lock.acquire();
        Thread.sleep(1000);
        lock.release();
    }

    @Test
    public void TReentrantLockTestTimes100() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        List<TReentrantLock> list = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            list.add(new TReentrantLock("path" + i));
        }
        for(int i = 0; i < 2; i++) {
            long begin = System.currentTimeMillis();
            for (TReentrantLock lock : list
            ) {
                lock.acquire();
            }
            long end = System.currentTimeMillis();
            System.out.println("---------------------acquire" + i + ":" + (end - begin) + "ms");
            for (TReentrantLock lock : list
            ) {
                lock.release();
            }
            long nexEnd = System.currentTimeMillis();
            System.out.println("---------------------release" + i + ":" + (nexEnd - end) + "ms");
        }
    }

    @Test
    public void TReentrantLockTest2() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        TReentrantLock lock = new TReentrantLock("path1");
        lock.acquire();
        Thread.sleep(1000);
        lock.acquire();
        Thread.sleep(1000);
        lock.release();
        lock.release();
    }

    @Test
    public void TReentrantReadWriteLockTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        TReentrantReadWriteLock readWriteLock = new TReentrantReadWriteLock("path1");
        readWriteLock.readLock();
        Thread.sleep(1000);
        readWriteLock.unlockRead();
        readWriteLock.writeLock();
        Thread.sleep(1000);
        readWriteLock.unlockWrite();
    }

    @Test
    public void TReentrantReadWriteLockTest2() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        TReentrantReadWriteLock readWriteLock = new TReentrantReadWriteLock("path2");
        readWriteLock.writeLock();
        readWriteLock.readLock();
        Thread.sleep(1000);
        readWriteLock.unlockRead();
        Thread.sleep(1000);
        readWriteLock.unlockWrite();
    }

    @Test
    public void TMultiReadWriteLockTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        List<String> readPaths = Arrays.asList("path3", "path4");
        List<String> writePaths = Arrays.asList("path5", "path6");
        TMultiReadWriteLock multiReadWriteLock = new TMultiReadWriteLock(readPaths, writePaths);
        multiReadWriteLock.acquire();
        Thread.sleep(1000);
        multiReadWriteLock.release();
    }

    @Test
    public void TMultiReadWriteLockTest100() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4087);
        List<String> writePath = new ArrayList<>();
        for(int i = 0; i < 100; i++) {
            writePath.add("path" + i);
        }
        for(int i = 0; i < 2; i++) {
            TMultiReadWriteLock multiReadWriteLock = new TMultiReadWriteLock(new ArrayList<>(), writePath);
            long begin = System.currentTimeMillis();
            multiReadWriteLock.acquire();
            long end = System.currentTimeMillis();
            System.out.println("---------------------acquire" + i + ":" + (end - begin) + "ms");
            multiReadWriteLock.release();
            long nexEnd = System.currentTimeMillis();
            System.out.println("---------------------release" + i + ":" + (nexEnd - end) + "ms");
        }
    }

    @Test
    public void longTimeoutTest() {
        if(run) {
            HashedWheelTimer timer = new HashedWheelTimer();
            timer.newTimeout(timeout -> {
                System.out.println("task run in：" + LocalDateTime.now());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignore) {
                }
            }, 1, TimeUnit.SECONDS);
            timer.newTimeout(timeout -> {
                System.out.println("task run in：" + LocalDateTime.now());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }, 1, TimeUnit.SECONDS);
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String line = scanner.next();
                if (line.equals("q")) {
                    break;
                }
            }
            scanner.close();
        }
    }

    @Test
    public void threadSingleLockTest2() throws Exception {
        threadSingleLockTestPort(4085);
    }

    @Test
    public void threadSingleLockTest() throws Exception {
        threadSingleLockTestPort(4087);
    }

    private void threadSingleLockTestPort(int port) throws Exception {
        threadPortFunction(port, ()->{
            TReentrantLock lock = new TReentrantLock("path1");
            lock.acquire();
            Thread.sleep(30);
            lock.release();
        });
    }

    @Test
    public void threadTReentrantLockTest2() throws Exception {
        threadTReentrantLockTest2Port(4085);
        /*Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String line = scanner.next();
            if (line.equals("q")) {
                break;
            }
        }
        scanner.close();*/
    }

    @Test
    public void threadTReentrantLockTest22() throws Exception {
        threadTReentrantLockTest2Port(4087);
    }

    private void threadTReentrantLockTest2Port(int port) throws Exception {
        threadPortFunction(port, ()-> {
            TReentrantLock lock = new TReentrantLock("path1");
            lock.acquire();
            Thread.sleep(30);
            lock.acquire();
            Thread.sleep(30);
            lock.release();
            lock.release();
        });
    }

    @Test
    public void threadTReentrantReadWriteLockTest() throws Exception {
        threadTReentrantReadWriteLockTestPort(4085);
    }

    @Test
    public void threadTReentrantReadWriteLockTest2() throws Exception {
        threadTReentrantReadWriteLockTestPort(4087);
    }

    private void threadTReentrantReadWriteLockTestPort(int port) throws Exception {
        threadPortFunction(port, ()-> {
            TReentrantReadWriteLock readWriteLock = new TReentrantReadWriteLock("path1");
            readWriteLock.readLock();
            Thread.sleep(30);
            readWriteLock.unlockRead();
            readWriteLock.writeLock();
            Thread.sleep(30);
            readWriteLock.unlockWrite();
        });
    }

    @Test
    public void threadTReentrantReadWriteLockTest21() throws Exception {
        threadTReentrantReadWriteLockTest2Port(4085);
    }

    @Test
    public void threadTReentrantReadWriteLockTest22() throws Exception {
        threadTReentrantReadWriteLockTest2Port(4087);
    }

    private void threadTReentrantReadWriteLockTest2Port(int port) throws Exception {
        threadPortFunction(port, ()-> {
            TReentrantReadWriteLock readWriteLock = new TReentrantReadWriteLock("path2");
            readWriteLock.writeLock();
            readWriteLock.readLock();
            Thread.sleep(30);
            readWriteLock.unlockRead();
            readWriteLock.unlockWrite();
        });
    }

    @Test
    public void threadTMultiReadWriteLockTest() throws Exception {
        threadTMultiReadWriteLockTestPort(4085);
    }

    @Test
    public void threadTMultiReadWriteLockTest2() throws Exception {
        threadTMultiReadWriteLockTestPort(4087);
    }

    @Test
    public void thread100LockTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(4097);
        ExecutorService service = Executors.newFixedThreadPool(200);
        CountDownLatch latch = new CountDownLatch(1000000);
        long begin = System.currentTimeMillis();
        AtomicLong atomicLong = new AtomicLong(0);
        for (int i = 0; i < 1000000; i++) {
            int finalI = i;
            service.submit(() -> {
                try {

                    TReentrantLock lock = new TReentrantLock("path" + finalI);
                    long lockBegin = System.currentTimeMillis();
                    lock.acquire();
                    lock.release();
                    long lockEnd = System.currentTimeMillis();
                    atomicLong.addAndGet(lockEnd - lockBegin);

                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println((end - begin) + "ms");
        System.out.println(atomicLong.get() + "ms");
    }


    private void threadTMultiReadWriteLockTestPort(int port) throws Exception {
        threadPortFunction(port, ()->{
            List<String> readPaths = Arrays.asList("path3", "path4");
            List<String> writePaths = Arrays.asList("path5", "path6");
            TMultiReadWriteLock multiReadWriteLock = new TMultiReadWriteLock(readPaths, writePaths);
            multiReadWriteLock.acquire();
            Thread.sleep(30);
            multiReadWriteLock.release();
        });
    }

    private void threadPortFunction(int port, CallBackException callback) throws Exception {
        if (run) {
            LockCoordinatorTest.LockCoordinatorInit(port);
            ExecutorService service = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(1000);
            for (int i = 0; i < 1000; i++) {
                service.submit(() -> {
                    try {
                        callback.apply();

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }

    }

}

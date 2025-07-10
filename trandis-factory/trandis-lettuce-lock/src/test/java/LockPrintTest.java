import com.xm.sanvanfo.lock.TReentrantLock;
import com.xm.sanvanfo.lock.TReentrantReadWriteLock;
import io.lettuce.core.AbstractRedisAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class LockPrintTest {

    private int i = 1;

    @Test
    public void addTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(8036);
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        long mills = System.currentTimeMillis();
        System.out.println(mills);
        final long[] i = {0};
        TReentrantLock lock = new TReentrantLock("path1");
        service.execute(() -> {
            for (; i[0] < 50000;) {
                try {
                    lock.acquire();
                    i[0]++;
                    lock.release();
                }
                catch (Exception ex) {ex.printStackTrace();}
            }
            latch.countDown();
        });
        service.execute(() -> {
            for (; i[0] < 50000;) {

                try {
                    lock.acquire();
                    i[0]++;
                    lock.release();
                }
                catch (Exception ex) {ex.printStackTrace();}

            }
            latch.countDown();
        });
        latch.await();
        System.out.println(System.currentTimeMillis() - mills);
    }

    @Test
    public void addTest2() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(8035);
        long mills = System.currentTimeMillis();
        System.out.println(mills);
        final long[] i = {0};
        TReentrantLock lock = new TReentrantLock("path1");
        for (; i[0] < 50000;) {
            try {
                lock.acquire();
                i[0]++;
                lock.release();
            }
            catch (Exception ex) {ex.printStackTrace();}
        }
        System.out.println(System.currentTimeMillis() - mills);
    }

    @Test
    public void lockPrint100Test() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(100);
        LockCoordinatorTest.LockCoordinatorInit(8087);
        for (int j = 0; j < 100; j++) {
            service.execute(() -> {
                TReentrantLock lock = new TReentrantLock("path1");
                try {
                    lock.acquire();
                    log.info(Thread.currentThread().getName() + "--------" + i);
                    Thread.sleep(1000);
                    i++;

                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        lock.release();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    latch.countDown();
                }

            });
        }
        latch.await();
    }

    @Test
    public void lockWritePrint100Test() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(100);
        LockCoordinatorTest.LockCoordinatorInit(8088);
        for (int j = 0; j < 100; j++) {
            service.execute(() -> {
                TReentrantReadWriteLock lock = new TReentrantReadWriteLock("path1");
                try {
                    lock.writeLock();
                    log.info(Thread.currentThread().getName() + "--------" + i);
                    Thread.sleep(1000);
                    i++;


                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        lock.unlockWrite();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    @Test
    public void lockReadPrint100Test() throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(100);
        LockCoordinatorTest.LockCoordinatorInit(8088);
        for (int j = 0; j < 100; j++) {
            service.execute(() -> {
                TReentrantReadWriteLock lock = new TReentrantReadWriteLock("path1");
                try {
                    lock.readLock();
                    log.info(Thread.currentThread().getName() + "--------" + i);
                    Thread.sleep(1000);
                    i++;


                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        lock.unlockRead();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    latch.countDown();
                }

            });
        }
        latch.await();
    }

    @Test
    public void interProcessTest() throws Exception {
        interProcessTest(8089);
    }

    @Test
    public void interProcessTest2() throws Exception {
        interProcessTest(8090);
    }

    @Test
    public void interProcessTest3() throws Exception {
        interProcessTest(8091);
    }

    private void interProcessTest(int port) throws Exception {
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(100);
        LockCoordinatorTest.LockCoordinatorInit(port);
        for (int j = 0; j < 100; j++) {
            service.execute(()->{
                TReentrantLock lock = new TReentrantLock("path1");
                try {
                    lock.acquire();
                    AbstractRedisAsyncCommands<byte[], byte[]> commands = RepairTest.getCommands();
                    long seed = commands.incr(RepairTest.getSerializer().toKey("seed")).get();
                    commands.lpush(RepairTest.getSerializer().toKey("log"),
                            RepairTest.getSerializer().toValue(String.format("%d-%s---------------%d", port, Thread.currentThread().getName(), seed))).get();
                    Thread.sleep(1000);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                finally {
                    try {
                        lock.release();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    latch.countDown();
                }
            });
        }
        latch.await();
    }
}

import com.xm.sanvanfo.AbstractRedisLocker;
import com.xm.sanvanfo.LettuceRedisLocker;
import com.xm.sanvanfo.LockCoordinator;
import com.xm.sanvanfo.common.utils.Asserts;
import com.xm.sanvanfo.lock.TReentrantLock;
import com.xm.sanvanfo.lock.TReentrantReadWriteLock;
import io.lettuce.core.AbstractRedisAsyncCommands;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.function.Function;

public class RepairTest {

    @Test
    public void lockRepairTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(5087);
        TReentrantLock lock = new TReentrantLock("path1");
        lock.acquire();
        AbstractRedisAsyncCommands<byte[], byte[]> commands = getCommands();
        commands.del(getSerializer().toKey("path1-{coordinator}-writeLock")).get();
        lock.release();
    }

    @Test
    public void lockRepairTest2() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(5087);
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        service.execute(()->{
            try {
                TReentrantLock lock = new TReentrantLock("path1");
                lock.acquire();
                System.out.println("thread1 acquire");
                service.execute(()->{
                    try {
                        Thread.sleep(2000);
                        TReentrantLock lock2 = new TReentrantLock("path1");
                        lock2.acquire();
                        System.out.println("thread2 acquire");
                        Thread.sleep(30);
                        lock2.release();
                        System.out.println("thread2 release");
                        latch.countDown();
                    }
                    catch (Exception ex) {
                          ex.printStackTrace();
                    }
                });
                AbstractRedisAsyncCommands<byte[], byte[]> commands = getCommands();
                commands.del(getSerializer().toKey("path1-{coordinator}-writeLock")).get();
                Thread.sleep(10000);
                lock.acquire();
                System.out.println("thread1 acquire2");
                Thread.sleep(30);
                lock.release();
                System.out.println("thread1 release");
                lock.release();
                System.out.println("thread1 release2");
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        latch.await();
    }

    @Test
    public void lockWaitRepairTest() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(5088);
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        service.execute(()->{
            try {
                TReentrantLock lock = new TReentrantLock("path1");
                lock.acquire();
                System.out.println("thread1 acquire");
                service.execute(()->{

                    TReentrantLock lock2 = new TReentrantLock("path1");
                    try {
                        lock2.acquire();
                        System.out.println("thread2 acquire");
                        Thread.sleep(5000);
                        lock2.release();
                        System.out.println("thread2 release");
                        latch.countDown();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                Thread.sleep(2000);
                AbstractRedisAsyncCommands<byte[], byte[]> commands = getCommands();
                commands.del(getSerializer().toKey("path1-{coordinator}-coordinatorLockWait")).get();
                lock.release();
                System.out.println("thread1 release");
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        latch.await();
    }

    @Test
    public void lockWaitRepairTest2() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(5089);
        ExecutorService service = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);
        service.execute(()->{
            try {
                TReentrantLock lock = new TReentrantLock("path1");
                lock.acquire();
                System.out.println("thread1 acquire");
                service.execute(()->{
                    TReentrantLock lock2 = new TReentrantLock("path1");
                    try {
                        lock2.acquire();
                        System.out.println("thread2 acquire");
                        Thread.sleep(5000);
                        lock2.release();
                        System.out.println("thread2 release");
                        latch.countDown();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                service.execute(()->{
                    TReentrantLock lock3 = new TReentrantLock("path1");
                    try {
                        lock3.acquire();
                        System.out.println("thread3 acquire");
                        Thread.sleep(5000);
                        lock3.release();
                        System.out.println("thread3 release");
                        latch.countDown();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
                Thread.sleep(2000);
                AbstractRedisAsyncCommands<byte[], byte[]> commands = getCommands();
                commands.lpop(getSerializer().toKey("path1-{coordinator}-coordinatorLockWait")).get();
                lock.release();
                System.out.println("thread1 release");
                latch.countDown();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        latch.await();
    }

    @Test
    public void readWriteLockReadRepair() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(5090);
        TReentrantReadWriteLock readWriteLock = new TReentrantReadWriteLock("path1");
        readWriteLock.readLock();
        AbstractRedisAsyncCommands<byte[], byte[]> commands = getCommands();
        commands.del(getSerializer().toKey("path1-{coordinator}-readLock")).get();
        try {
            boolean ret = readWriteLock.writeLock(10L, TimeUnit.SECONDS);
            System.out.println("write lock result" + ret);
        }
        catch (TimeoutException ex) {
            ex.printStackTrace();
        }
        finally {
            try {
                readWriteLock.unlockWrite();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        readWriteLock.unlockRead();
    }

    @Test
    public void readWriteLockReadRepair2() throws Exception {
        LockCoordinatorTest.LockCoordinatorInit(5091);
        ExecutorService service = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        TReentrantReadWriteLock readWriteLock = new TReentrantReadWriteLock("path1");
        readWriteLock.writeLock();
        System.out.println("thread 1 write acquire");
        service.execute(()-> {

            TReentrantReadWriteLock readWriteLock2 = new TReentrantReadWriteLock("path1");
            try {
                AbstractRedisAsyncCommands<byte[], byte[]> commands = getCommands();
                commands.del(getSerializer().toKey("path1-{coordinator}-writeLock")).get();
                readWriteLock2.writeLock();
                System.out.println("thread 2 write acquire");
                readWriteLock2.readLock();
                System.out.println("thread 2 read acquire");
                Thread.sleep(2000);
                readWriteLock2.unlockRead();
                System.out.println("thread 2 read release");
                readWriteLock2.unlockWrite();
                System.out.println("thread 2 write release");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            latch.countDown();
        });
        readWriteLock.readLock();
        System.out.println("thread 1 read acquire");
        Thread.sleep(5000);
        readWriteLock.unlockRead();
        System.out.println("thread 1 read release");
        readWriteLock.unlockWrite();
        System.out.println("thread 1 write release");
        latch.countDown();
        latch.await();
    }

    public static AbstractRedisAsyncCommands<byte[], byte[]> getCommands() throws Exception {
       Field field =  LockCoordinator.class.getDeclaredField("locker");
       field.setAccessible(true);
       Object obj = field.get(LockCoordinator.INSTANCE());
        Asserts.isTrue(obj instanceof LettuceRedisLocker);
        LettuceRedisLocker<byte[], byte[]> locker = (LettuceRedisLocker)obj;
        Field commandField = LettuceRedisLocker.class.getDeclaredField("connection");
        commandField.setAccessible(true);
        return (AbstractRedisAsyncCommands)commandField.get(locker);
    }

    public static com.xm.sanvanfo.interfaces.Serializer<byte[],byte[]> getSerializer() throws Exception {
        Field field =  LockCoordinator.class.getDeclaredField("locker");
        field.setAccessible(true);
        Object obj = field.get(LockCoordinator.INSTANCE());
        Asserts.isTrue(obj instanceof LettuceRedisLocker);
        LettuceRedisLocker<byte[], byte[]> locker = (LettuceRedisLocker)obj;
        Field serializerField =  AbstractRedisLocker.class.getDeclaredField("redisSerializer");
        serializerField.setAccessible(true);
        return (com.xm.sanvanfo.interfaces.Serializer<byte[],byte[]>) serializerField.get(locker);
    }
}

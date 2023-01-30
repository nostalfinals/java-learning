package ltd.kumo.javalearning.juc;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁测试。
 */
public class LockTest {
    static AtomicInteger integer = new AtomicInteger(0);

    public static void main(String[] args) {
        final Lock lock = new ReentrantLock();
        AtomicInteger number = new AtomicInteger();
        final ExecutorService executorService = Executors.newCachedThreadPool();

        while (number.get() < 100) {
            executorService.submit(() -> {
                try {
                    lock.lock();
                    if (number.get() < 100) {
                        number.getAndIncrement();
                        System.out.println(Thread.currentThread() + " - " + number.get());
                    }
                } finally {
                    lock.unlock();
                }
            });
        }

        executorService.shutdown();
    }
}

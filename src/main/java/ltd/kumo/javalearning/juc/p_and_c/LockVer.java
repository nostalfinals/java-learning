package ltd.kumo.javalearning.juc.p_and_c;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 生产者消费者 Lock 实现。
 * 判断 -> 等待 -> 任务 -> 通知。
 */
public final class LockVer {
    // 入口方法。
    public static void main(String[] args) {
        Data1 data1 = new Data1();

        new Thread(() -> doTaskAdd(data1), "A").start();
        new Thread(() -> doTaskRemove(data1), "B").start();
        new Thread(() -> doTaskAdd(data1), "C").start();
        new Thread(() -> doTaskRemove(data1), "D").start();
    }

    private static void doTaskAdd(Data1 data) {
        for (int i = 0; i < 20; i++) {
            try {
                data.add();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void doTaskRemove(Data1 data) {
        for (int i = 0; i < 20; i++) {
            try {
                data.remove();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}

// 资源类。
class Data1 {
    Lock lock = new ReentrantLock();
    Condition condition = lock.newCondition();
    private int number = 0;

    // +1
    public void add() throws InterruptedException {
        lock.lock();

        try {
            while (number == 1) { // while 循环判断，防止虚假唤醒。
                condition.await(); // 其他线程还未操作完毕，等待。
            }

            number++;
            System.out.println(Thread.currentThread() + " - " + number);
            condition.signalAll(); // 通知其他线程任务已完成。
        } finally {
            lock.unlock();
        }
    }

    // -1
    public void remove() throws InterruptedException {
        lock.lock();

        try {
            while (number != 1) {
                condition.await();
            }

            number--;
            System.out.println(Thread.currentThread() + " - " + number);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}

package ltd.kumo.javalearning.juc.p_and_c;


/**
 * 生产者消费者 Synchronized 实现。
 * 判断 -> 等待 -> 任务 -> 通知。
 */
public final class SynchronizedVer {
    // 入口方法。
    public static void main(String[] args) {
        Data data = new Data();

        new Thread(() -> doTaskAdd(data), "A").start();
        new Thread(() -> doTaskRemove(data), "B").start();
        new Thread(() -> doTaskAdd(data), "C").start();
        new Thread(() -> doTaskRemove(data), "D").start();
    }

    private static void doTaskAdd(Data data) {
        for (int i = 0; i < 20; i++) {
            try {
                data.add();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void doTaskRemove(Data data) {
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
class Data {
    private int number = 0;

    // +1
    public synchronized void add() throws InterruptedException {
        while (number == 1) { // while 循环判断，防止虚假唤醒。
            this.wait(); // 其他线程还未操作完毕，等待。
        }

        number++;
        System.out.println(Thread.currentThread() + " - " + number);
        this.notifyAll(); // 通知其他线程任务已完成。
    }

    // -1
    public synchronized void remove() throws InterruptedException {
        while (number != 1) {
            this.wait();
        }

        number--;
        System.out.println(Thread.currentThread() + " - " + number);
        this.notifyAll();
    }
}

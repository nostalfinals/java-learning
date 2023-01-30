package ltd.kumo.javalearning.juc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class WorkStealingExecutor {
    private final List<WorkerThread> workers = new ArrayList<>();
    private final Lock workersLock = new ReentrantLock();
    private int lastPostedPos = 0;

    public WorkStealingExecutor(int nThreads) {
        this.runWorkers(nThreads);
    }

    public static void main(String[] args) {
        final WorkStealingExecutor workStealingExecutor = new WorkStealingExecutor(8);
        for (int i = 0; i < 52; i++) {
            workStealingExecutor.postTask(() -> {
                try {
                    Thread.sleep(200);
                    System.out.println(Thread.currentThread());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        //workStealingExecutor.awaitTasks();
        workStealingExecutor.stopAll();
    }

    public void stopAll() {
        this.workersLock.lock();
        try {
            for (WorkerThread workerThread : this.workers) {
                workerThread.sendStop();
                workerThread.awaitTerminate();
            }
            this.workers.clear();
        } finally {
            this.workersLock.unlock();
        }
    }

    private void runWorkers(int threadCount) {
        for (int i = 0; i < threadCount; i++) {
            final WorkerThread workerThread = new WorkerThread();
            this.workersLock.lock();
            try {
                this.workers.add(workerThread);
            } finally {
                this.workersLock.unlock();
            }
            workerThread.start();
        }
    }

    public void awaitTasks() {
        this.workersLock.lock();
        try {
            for (WorkerThread workerThread : this.workers) {
                workerThread.awaitTasks();
            }
        } finally {
            this.workersLock.unlock();
        }
    }

    public void postTask(Runnable task) {
        this.workersLock.lock();
        try {
            if (this.lastPostedPos + 1 >= this.workers.size()) {
                this.lastPostedPos = 0;
            }
            final WorkerThread workerThread = this.workers.get(this.lastPostedPos++);
            workerThread.postRunnable(task);
        } finally {
            this.workersLock.unlock();
        }
    }

    public <T> Future<T> postTask(Callable<T> task) {
        final FutureTask<T> newTask = new FutureTask<>(task);
        this.postTask(newTask);
        return newTask;
    }


    private class WorkerThread extends Thread {
        private final Deque<Runnable> tasks = new ArrayDeque<>();
        private final Lock taskLock = new ReentrantLock();
        private final AtomicInteger runningTask = new AtomicInteger();

        private volatile boolean shouldRun = false;
        private volatile boolean running = false;

        public void postRunnable(Runnable task) {
            this.taskLock.lock();
            try {
                this.tasks.add(task);
            } finally {
                this.taskLock.unlock();
            }
        }

        public void sendStop() {
            this.shouldRun = false;
            LockSupport.unpark(this);
        }

        public void awaitTerminate() {
            while (this.running) {
                LockSupport.parkNanos(100000);
            }
        }

        public void awaitTasks() {
            while (this.runningTask.get() > 0) {
                LockSupport.parkNanos(100000);
            }
        }

        @Override
        public void run() {
            Runnable curTask;
            Runnable stole;
            while (this.shouldRun) {
                if ((curTask = this.pollTask(false)) != null) {
                    this.runningTask.getAndIncrement();
                    try {
                        curTask.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        this.runningTask.getAndDecrement();
                    }
                    continue;
                }

                if ((stole = this.steal()) != null) {
                    try {
                        stole.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                LockSupport.parkNanos("FREE WAITING", 1000000);
            }
            this.running = false;
        }

        @Override
        public void start() {
            this.running = true;
            this.shouldRun = true;
            super.start();
        }

        private Runnable steal() {
            WorkStealingExecutor.this.workersLock.lock();
            try {
                for (WorkerThread workerThread : WorkStealingExecutor.this.workers) {
                    if (workerThread.equals(this)) {
                        continue;
                    }
                    if (workerThread.getRunningCount() > 0 && workerThread.getTasks() >= 1) {
                        return workerThread.pollTask(true);
                    }
                }
            } finally {
                WorkStealingExecutor.this.workersLock.unlock();
            }
            return null;
        }


        protected int getTasks() {
            this.taskLock.lock();
            try {
                return this.tasks.size();
            } finally {
                this.taskLock.unlock();
            }
        }

        protected int getRunningCount() {
            return this.runningTask.get();
        }

        protected Runnable pollTask(boolean tail) {
            Runnable polledRunnable;

            this.taskLock.lock();
            try {
                polledRunnable = tail ? this.tasks.pollLast() : this.tasks.pollFirst();
            } finally {
                this.taskLock.unlock();
            }

            return polledRunnable;
        }
    }
}

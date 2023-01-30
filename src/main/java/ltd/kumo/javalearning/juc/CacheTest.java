package ltd.kumo.javalearning.juc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CacheTest {
    public static void main(String[] args) throws InterruptedException {
        // 缓存对象
        Cache<String, Object> caffeine = Caffeine.newBuilder().build();

        // 存储数据
        long start = System.currentTimeMillis();
        Lock lock = new ReentrantLock();
        // ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        WorkStealingExecutor workStealingExecutor = new WorkStealingExecutor(Runtime.getRuntime().availableProcessors());
        int count = 0;
        Random random = new Random();
        while (count < 10000000) {
            workStealingExecutor.postTask(() -> {
                try {
                    lock.lock();
                    caffeine.put(String.valueOf(random.nextDouble()), random.nextInt());
                } finally {
                    lock.unlock();
                }
            });
            count++;
        }

        long end = System.currentTimeMillis();

        while (true) {
            if (caffeine.asMap().size() == 10000000) {
                break;
            }
        }

        System.out.println(count);
        System.out.println("存储数据: " + caffeine.asMap().size());
        System.out.println("用时: " + (end - start));

        // executorService.shutdown();

        Map<String, Object> map = caffeine.asMap();

        long filterStart = System.currentTimeMillis();

        var collection = map.entrySet().stream()
                .parallel()
                .filter(stringObjectEntry -> Double.parseDouble(stringObjectEntry.getKey()) < 0.1)
                .toList();

        long filterEnd = System.currentTimeMillis();

        System.out.println(collection.size());
        System.out.println("筛选数字用时: " + (filterEnd - filterStart));
    }
}

package io.best.tool.cache;


import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class LocalCache {

    private final static String VALUE = "cache test value";

    private static ThreadPoolExecutor cachePoll = new ThreadPoolExecutor(10, 50, 300, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(1000), new ThreadFactoryBuilder()
            .setNameFormat("cache-pool-%d").build(), new ThreadPoolExecutor.DiscardOldestPolicy());

    /**
     * 构建方法
     */
    LoadingCache<String, String> localCache = CacheBuilder.newBuilder()
            .maximumSize(1000)//最大size 超出之后，会进行回收，LUR算法
            .expireAfterAccess(1, TimeUnit.SECONDS)//expireAfterAccess: 在指定的过期时间内没有读写，缓存数据即失效
            .expireAfterWrite(1, TimeUnit.SECONDS)//expireAfterWrite: 在指定的过期时间内没有写入，缓存数据即失效
            .refreshAfterWrite(1, TimeUnit.SECONDS)//refreshAfterWrite: 在指定的过期时间之后访问时，刷新缓存数据，在刷新任务未完成之前，其他线程返回旧值
            .recordStats()
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String s) throws Exception {
                    //cache get 不存在，则load写入缓存，并返回。这里具备原子性，高并发情况下会阻塞大量的线程
                    return VALUE;
                }

                @Override
                public ListenableFuture<String> reload(String key, String oldValue) throws Exception {
                    ListenableFutureTask<String> task = ListenableFutureTask.create(new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return VALUE;
                        }
                    });
                    cachePoll.execute(task);
                    return task;
                }
            });
}

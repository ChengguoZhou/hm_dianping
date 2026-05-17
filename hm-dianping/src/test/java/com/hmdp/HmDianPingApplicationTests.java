package com.hmdp;

import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Resource
    private RedisIdWorker redisIdWorker;

    // 创建线程池
    private ExecutorService es = Executors.newFixedThreadPool(500);

    /**
     * 单元测试保存店铺功能
     */
    @Test
    void testSaveShop() throws InterruptedException {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.CACHE_SHOP_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }

    /**
     * 并发测试 redisIdWorker.nextId("order") 生成全局唯一 ID 的性能和安全性。
     * 300 个线程任务，每个任务生成 100 个订单 ID
     * 总共生成 300 × 100 = 30000 个 ID
     * @throws InterruptedException
     */
    @Test
    void testIdWorker() throws InterruptedException {
        // CountDownLatch 可以理解成一个倒计时门闩。
        //这里初始值是 300，表示主线程要等待 300 个任务全部执行完成。
        CountDownLatch latch = new CountDownLatch(300);

        // 定义一个任务
        Runnable task = () ->{
            for (int i = 0; i < 100; ++i){
                long order = redisIdWorker.nextId("order");
                System.out.println(order);
            }
            // 任务结束后执行countdown
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        // 向线程池提交300个任务
        for (int i = 0; i < 300; ++i){
            es.submit(task);
        }
        // 阻塞当前线程直到所有子任务执行完，再统计结束时间
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time = " + (end - begin));
    }
}

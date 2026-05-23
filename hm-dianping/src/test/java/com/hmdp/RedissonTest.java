package com.hmdp;

import com.hmdp.utils.RedisConstants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * RedissonTest
 *
 * <p>功能描述：Redisson锁</p>
 *
 * @author 19808
 * @since 2026/5/21
 */
@Slf4j
@SpringBootTest(properties = "hmdp.redisson.multi.enabled=true")
@EnabledIfSystemProperty(named = "redisson.multi.test", matches = "true")
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedissonClient redissonClient1;
    @Resource
    private RedissonClient redissonClient2;

    private RLock lock;

    @BeforeEach
    void setUp() {
        RLock lock1 = redissonClient.getLock(RedisConstants.ORDER);
        RLock lock2 = redissonClient1.getLock(RedisConstants.ORDER);
        RLock lock3 = redissonClient2.getLock(RedisConstants.ORDER);
        // 创建联锁
        lock = redissonClient.getMultiLock(lock1, lock2, lock3);

    }

    @Test
    void method1() throws InterruptedException {
        // 尝试获取锁
        boolean isLock = lock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock){
            log.error("获取锁失败 ... 1");
            return;
        }
        try {
            log.info("获取锁成功 ... 1");
            mothod2();
            log.info("开始执行业务 ... 1");
        } finally {
            log.warn("准备释放锁 ... 1");
            lock.unlock();
        }
    }

    private void mothod2() {
        // 尝试获取锁
        boolean isLock = lock.tryLock();
        if (!isLock){
            log.error("获取锁失败 ... 2");
            return;
        }
        try {
            log.info("获取锁成功 ... 2");
            log.info("开始执行业务 ... 2");
        } finally {
            log.warn("准备释放锁 ... 2");
            lock.unlock();
        }
    }
}

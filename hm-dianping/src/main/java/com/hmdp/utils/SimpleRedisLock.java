package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * SimpleRedisLock
 *
 * <p>功能描述：</p>
 *
 * @author 19808
 * @since 2026/5/19
 */
public class SimpleRedisLock implements ILock{

    // 锁的名称，不同业务锁的名称不同
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private static final String KEY_PREFIX = "lock:";

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识
        long threadId = Thread.currentThread().getId();
        // 获取锁
        Boolean sussess = stringRedisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + name, threadId + "", timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(sussess);
    }

    @Override
    public void unlock() {
        // 释放锁
        stringRedisTemplate.delete(KEY_PREFIX + name);
    }
}
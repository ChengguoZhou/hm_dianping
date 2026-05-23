package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * SimpleRedisLock
 *
 * <p>功能描述：Redis锁简单实现</p>
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
    // 锁前缀
    private static final String KEY_PREFIX = "lock:";
    // 线程前缀
    public static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    // 释放锁脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static{
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("lua/unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标识(UUID + 线程ID)
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁
        Boolean sussess = stringRedisTemplate.opsForValue().setIfAbsent(
                KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(sussess);
    }

    /**
     * 通过lua锁释放锁
     */
    @Override
    public void unlock() {
        // 调用lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
                );
    }

    /*
    @Override
    public void unlock() {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中的标识
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        // 判断标识是否一致
        if (threadId.equals(id)){
            // 释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
    */

}
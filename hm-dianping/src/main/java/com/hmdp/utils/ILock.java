package com.hmdp.utils;

/**
 * ILock
 *
 * <p>接口描述：利用Redis实现分布式锁功能</p>
 *
 * @author 19808
 * @since 2026/5/19
 */
public interface ILock {
    /**
     * 尝试获取锁
     * @param timeoutSec 锁持有的超时时间，过期自动释放
     * @return 布尔值，是否获取锁成功
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
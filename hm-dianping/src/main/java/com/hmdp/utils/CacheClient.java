package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * CacheClient
 *
 * <p>功能描述：Redis缓存工具类</p>
 *
 * @author 19808
 * @since 2026/5/16
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 定义全局固定大小的线程池，用来专门执行异步任务
    public static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 将Object序列化后，添加到Redis缓存数据
     * @param key 关键词key
     * @param value 待添加对象value
     * @param time 有效时间
     * @param unit 有效时间单位
     */
    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     * 采用逻辑过期的方式，往Redis添加缓存数据
     * @param key 关键词key
     * @param value 待添加对象value
     * @param time 有效时间
     * @param unit 有效时间单位
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * 避免缓存穿透的查询方法
     * @param keyPrefix Redis缓存key前缀
     * @param id 查询关键字key
     * @param type 返回值数据类型type
     * @param dbFallback 数据库回退方法
     * @param time
     * @param unit
     * @return
     * @param <R>
     * @param <ID>
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断Redis查询到的数据是否存在
        if (StrUtil.isNotBlank(json)){
            // 3.存在，直接返回
            return JSONUtil.toBean(json, type);
        }
        // 判断Redis命中的数据是否是空值/空字符串
        if (json != null){
            // Redis命中的商铺信息是空字符串（避免缓存穿透手动添加的），返回一个null
            return null;
        }
        // 4.Redis未命中，根据id查询数据库
        R r = dbFallback.apply(id);
        // 5.查询数据库不存在，保存空字符串到Redis并返回错误
        if (r == null){
            // 将空值写入Redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // 6.查询数据库存在，写入redis
        this.set(key, r, time, unit);
        // 7.返回
        return r;
    }


    /**
     * 通过逻辑有效期避免缓存击穿的查询方法
     * @param keyPrefix Redis中key的前缀
     * @param lockKeyPrefix 线程锁前缀
     * @param id 查询id
     * @param type 查询对象类型
     * @param dbFallback 数据库查询回退方法
     * @param time 有效时间
     * @param unit 有效时间单位
     * @return 查询对象实体类
     * @param <R> 泛型，查询返回对象类型
     * @param <ID> 泛型，查询ID类型
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, String lockKeyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
            Long time, TimeUnit unit){
        String key = keyPrefix + id;
        // 1.从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2.判断Redis查询到的商铺信息是否存在
        if (StrUtil.isBlank(json)){
            // 3.不存在，直接返回null
            return null;
        }
        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            // 5.1 未过期，直接返回店铺信息
            return r;
        }
        // 5.2 已过期，需要缓存重建
        // 6.缓存重建
        // 6.1 获取互斥锁
        String lockKey = lockKeyPrefix + id;
        boolean isLock = tryLock(lockKey);
        // 6.2 判断是否获取锁成功
        if (isLock){
            // 6.3 成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(()->{
                try {
                    // 重建缓存
                    R r1 = dbFallback.apply(id);
                    // 写入Redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4 返回过期的数据信息
        return r;
    }

    /**
     * 获取锁，用setnx命令实现类似功能
     * @param key setnx变量的名称
     * @return 获取成功或失败
     */
    public boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", CACHE_NULL_TTL, TimeUnit.MINUTES);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放锁
     * @param key setnx变量的名称
     */
    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }
}
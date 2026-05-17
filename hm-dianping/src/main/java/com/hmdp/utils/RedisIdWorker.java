package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * RedisIdWorker
 *
 * <p>功能描述：基于Redis的ID生成器</p>
 *
 * @author 19808
 * @since 2026/5/16
 */
@Component
public class RedisIdWorker {
    // 开始时间戳（2022-1-1 0:0:0）
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    // 序列号位数（32位）
    private static final int COUNT_BITS = 32;

    private StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long nextId(String keyPrefix){
        // 1、生成时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;

        // 2、生成序列号(利用Redis的自增长)
        // 2.1 获取当前日期，精确到天(避免超过2^32上限；方便做统计)
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        long count = stringRedisTemplate.opsForValue().increment(
                RedisConstants.ICR + keyPrefix + ":" + date);
        // 3、拼接并返回(考虑到实际每秒钟订单数不超过2^32，可以利用或运算节省时间)
        return timestamp << COUNT_BITS | count;
    }

    public static void main(String[] args) {
        LocalDateTime time = LocalDateTime.of(2022, 1, 1, 0, 0, 0);
        // 把 2022-01-01 00:00:00 当成 UTC（协调世界时时区） 时间 来计算时间戳
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second);

    }
}
package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.hmdp.utils.SystemConstants.REDIS_ADDRESS;
import static com.hmdp.utils.SystemConstants.REDIS_PASSWORD;

/**
 * RedissonConfig
 *
 * <p>功能描述：redisson配置文件</p>
 *
 * @author 19808
 * @since 2026/5/19
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_ADDRESS).setPassword(REDIS_PASSWORD);

        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
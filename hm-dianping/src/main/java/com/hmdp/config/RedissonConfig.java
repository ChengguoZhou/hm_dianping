package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.hmdp.utils.SystemConstants.*;

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
        config.useSingleServer().setAddress(REDIS_ADDRESS_6379).setPassword(REDIS_PASSWORD);

        // 创建RedissonClient对象
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "hmdp.redisson.multi", name = "enabled", havingValue = "true")
    public RedissonClient redissonClient1(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_ADDRESS_6380);

        // 创建RedissonClient对象
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnProperty(prefix = "hmdp.redisson.multi", name = "enabled", havingValue = "true")
    public RedissonClient redissonClient2(){
        // 配置
        Config config = new Config();
        config.useSingleServer().setAddress(REDIS_ADDRESS_6381);

        // 创建RedissonClient对象
        return Redisson.create(config);
    }

}

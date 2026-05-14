package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * MvcConfig MVC 配置类
 * <p>
 * 功能描述：用于注册和配置 Spring MVC 相关组件，例如拦截器、跨域配置、静态资源映射等。
 * 当前主要用于配置登录校验拦截器，拦截需要登录后才能访问的接口。
 *
 * @author 19808
 * @since 2026/5/10
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 登录拦截器
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/user/code", "/user/login", "/blog/hot", "/shop/**", "/shop-type/**", "/voucher/**", "/upload/**"
        ).order(1);
        // token刷新拦截器
        // order 优先级（值越小，优先级越高）
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
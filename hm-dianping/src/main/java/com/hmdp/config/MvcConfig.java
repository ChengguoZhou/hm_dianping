package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MvcConfig MVC 配置类
 *
 * 功能描述：用于注册和配置 Spring MVC 相关组件，例如拦截器、跨域配置、静态资源映射等。
 *  当前主要用于配置登录校验拦截器，拦截需要登录后才能访问的接口。
 * @author 19808
 * @since 2026/5/10
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/user/code", "/user/login", "/blog/hot", "/shop/**", "/shop-type/**", "/voucher/**", "/upload/**"
        );
    }
}
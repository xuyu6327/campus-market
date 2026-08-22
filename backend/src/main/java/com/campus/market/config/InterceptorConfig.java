package com.campus.market.config;

import com.campus.market.common.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 拦截器注册配置
 *  - 将 JWT 拦截器注册到 Spring MVC 拦截链中
 *  - 指定哪些接口需要登录（拦截），哪些不需要（放行）
 *  - 放行接口：登录、注册、商品列表/详情/搜索（可选认证）、API 文档等
 */
@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 放行以下路径（不需要登录）
                .excludePathPatterns(
                        // 登录/注册（无需认证）
                        "/user/login",
                        "/user/register",
                        // 商品分类（完全公开）
                        "/goods/category",
                        // 公开信息接口（用户主页/用户评价/用户在售商品）
                        "/user/*/profile",
                        "/goods/user/*",
                        "/review/user/*",
                        // 前端页面（静态资源）
                        "/",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js",
                        "/*.html",
                        "/*.css",
                        "/*.js",
                        "/favicon.ico",
                        // 错误页面（避免404时被拦截）
                        "/error",
                        // API 文档
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        // 上传的图片等
                        "/static/**",
                        "/images/**",
                        "/uploads/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j 静态资源映射
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/swagger-ui/");
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // 上传文件静态资源映射（商品图片等）
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:./uploads/");
    }
}

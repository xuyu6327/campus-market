package com.campus.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置（CORS）
 *  - 前端 uni-app 小程序和 Web 端都需要调用后端 API
 *  - 开发阶段允许所有来源，生产环境应限制为具体域名
 *  - 允许携带 Authorization 请求头（JWT token）
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许任何域名访问
        config.addAllowedOriginPattern("*");
        // 允许发送 Cookie
        config.setAllowCredentials(true);
        // 允许任何请求头
        config.addAllowedHeader("*");
        // 允许任何 HTTP 方法
        config.addAllowedMethod("*");
        // 允许携带 Authorization 请求头
        config.addExposedHeader("Authorization");
        // 缓存预检请求结果 3600 秒
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}

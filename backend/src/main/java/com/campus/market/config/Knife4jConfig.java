package com.campus.market.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j API 文档配置（基于 springdoc-openapi3）
 *  - 后端启动后，访问 http://localhost:8080/api/doc.html 查看 API 接口文档
 *  - 前端开发时，直接查看接口定义、参数、返回值，无需看代码
 *  - 比原始 Swagger 更美观，支持调试接口
 *
 * 注意：Knife04j 4.x 基于 springdoc-openapi，不再使用 springfox 的 Docket
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "校园二手交易平台",
                description = "齐鲁工业大学校内闲置物品交易平台 REST API 文档",
                version = "1.0.0",
                contact = @Contact(name = "齐鲁工业大学", url = "https://www.qlu.edu.cn", email = "support@qlu.edu.cn")
        )
)
public class Knife4jConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("campus-market")
                .packagesToScan("com.campus.market.controller")
                .build();
    }
}

package com.campus.market;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手交易平台 - 启动类
 *
 * @SpringBootApplication  Spring Boot 自动配置入口
 * @MapperScan             扫描 Mapper 接口所在包，MyBatis-Plus 自动生成实现类
 * @EnableScheduling       开启定时任务支持（商品自动下架、订单超时释放等）
 */
@SpringBootApplication
@MapperScan("com.campus.market.mapper")
@EnableScheduling
public class CampusMarketApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CampusMarketApplication.class, args);
        // 动态读取端口，避免硬编码
        String port = context.getEnvironment().getProperty("server.port", "8080");
        System.out.println("\n" +
                "========================================\n" +
                "  校园二手交易平台 启动成功!\n" +
                "  Web 端:   http://localhost:" + port + "/\n" +
                "  API 文档: http://localhost:" + port + "/doc.html\n" +
                "========================================\n");
    }
}

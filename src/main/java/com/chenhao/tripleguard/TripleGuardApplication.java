package com.chenhao.tripleguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Triple Guard 告警聚合平台启动类
 * <p>
 * 集成多数据源告警，提供统一的告警接收、存储和查询服务
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class TripleGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripleGuardApplication.class, args);
    }
}

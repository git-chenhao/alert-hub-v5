package com.alerthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Alert Hub V5 - 统一告警聚合平台
 *
 * 核心功能:
 * - HTTP Webhook 接收告警
 * - 告警去重（基于指纹算法）
 * - 攒批聚合（可配置时间窗口）
 * - A2A 调度分析
 * - 飞书通知推送
 */
@SpringBootApplication
@EnableScheduling
public class AlertHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertHubApplication.class, args);
    }
}

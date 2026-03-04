package com.alerthub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Alert Hub V5 - 统一告警聚合平台
 *
 * @author AlertHub Team
 * @version 5.0
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class AlertHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertHubApplication.class, args);
    }
}

package com.alerthub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 告警中心配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert")
public class AlertHubProperties {

    private DeduplicationConfig deduplication = new DeduplicationConfig();
    private AggregationConfig aggregation = new AggregationConfig();
    private A2AConfig a2a = new A2AConfig();
    private FeishuConfig feishu = new FeishuConfig();
    private SecurityConfig security = new SecurityConfig();
    private List<String> fingerprintFields = List.of("source", "alertName", "severity", "labels");

    @Data
    public static class DeduplicationConfig {
        private boolean enabled = true;
        private String algorithm = "MD5";
        private int ttlHours = 24;
    }

    @Data
    public static class AggregationConfig {
        private boolean enabled = true;
        private int windowSeconds = 60;
        private int maxBatchSize = 100;
        private String strategy = "TIME_WINDOW";
    }

    @Data
    public static class A2AConfig {
        private boolean enabled = true;
        private String agentUrl = "http://localhost:8081/api/agent";
        private int timeoutSeconds = 30;
        private int retryCount = 3;
    }

    @Data
    public static class FeishuConfig {
        private boolean enabled = false;
        private String webhookUrl;
        private String secret;
    }

    @Data
    public static class SecurityConfig {
        private String apiKey;
        private boolean apiKeyEnabled = true;
        private String dashboardPassword;
        private boolean dashboardAuthEnabled = true;
    }
}

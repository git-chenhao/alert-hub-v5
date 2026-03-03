package com.alerthub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Alert Hub 配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alerthub")
public class AlertHubProperties {

    /**
     * 聚合配置
     */
    private AggregationConfig aggregation = new AggregationConfig();

    /**
     * 去重配置
     */
    private DeduplicationConfig deduplication = new DeduplicationConfig();

    /**
     * 分析服务配置
     */
    private AnalysisConfig analysis = new AnalysisConfig();

    /**
     * 通知配置
     */
    private NotificationConfig notification = new NotificationConfig();

    @Data
    public static class AggregationConfig {
        /**
         * 聚合时间窗口（分钟）
         */
        private int windowMinutes = 5;

        /**
         * 是否启用聚合
         */
        private boolean enabled = true;
    }

    @Data
    public static class DeduplicationConfig {
        /**
         * 缓存 TTL（分钟）
         */
        private int cacheTtlMinutes = 60;
    }

    @Data
    public static class AnalysisConfig {
        /**
         * 是否启用分析
         */
        private boolean enabled = false;

        /**
         * 分析服务端点
         */
        private String endpoint = "http://localhost:8081/api/analyze";

        /**
         * 超时时间（秒）
         */
        private int timeoutSeconds = 30;
    }

    @Data
    public static class NotificationConfig {
        /**
         * 飞书通知配置
         */
        private FeishuConfig feishu = new FeishuConfig();
    }

    @Data
    public static class FeishuConfig {
        /**
         * 是否启用飞书通知
         */
        private boolean enabled = false;

        /**
         * Webhook URL
         */
        private String webhookUrl;

        /**
         * 最低通知级别
         */
        private String minSeverity = "MEDIUM";
    }
}

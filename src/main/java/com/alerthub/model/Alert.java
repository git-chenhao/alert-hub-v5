package com.alerthub.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 告警实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_received_at", columnList = "receivedAt"),
    @Index(name = "idx_source", columnList = "source"),
    @Index(name = "idx_batch_id", columnList = "batchId")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警指纹（用于去重）
     */
    @Column(nullable = false, length = 64)
    private String fingerprint;

    /**
     * 告警来源
     */
    @Column(nullable = false, length = 100)
    private String source;

    /**
     * 严重级别
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    /**
     * 告警标题
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * 告警描述
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 标签（用于分组和过滤）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    @Builder.Default
    private Map<String, String> labels = new HashMap<>();

    /**
     * 原始 JSON 数据
     */
    @Column(columnDefinition = "TEXT")
    private String rawPayload;

    /**
     * 告警状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.RECEIVED;

    /**
     * 接收时间
     */
    @Column(nullable = false)
    private LocalDateTime receivedAt;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 所属批次 ID
     */
    private Long batchId;

    /**
     * 重试次数
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * 错误信息
     */
    @Column(length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onPrePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (labels == null) {
            labels = new HashMap<>();
        }
    }
}

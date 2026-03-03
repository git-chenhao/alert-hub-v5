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
 * 告警批次实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alert_batches", indexes = {
    @Index(name = "idx_batch_key", columnList = "batchKey"),
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_window_start", columnList = "windowStart"),
    @Index(name = "idx_window_end", columnList = "windowEnd")
})
public class AlertBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 批次标识（source + severity + 时间窗口）
     */
    @Column(nullable = false, length = 200, unique = true)
    private String batchKey;

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
     * 时间窗口开始时间
     */
    @Column(nullable = false)
    private LocalDateTime windowStart;

    /**
     * 时间窗口结束时间
     */
    @Column(nullable = false)
    private LocalDateTime windowEnd;

    /**
     * 告警数量
     */
    @Column(nullable = false)
    @Builder.Default
    private int alertCount = 0;

    /**
     * A2A 分析结果
     */
    @Column(columnDefinition = "TEXT")
    private String analysisResult;

    /**
     * 批次状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BatchStatus status = BatchStatus.AGGREGATING;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 分析完成时间
     */
    private LocalDateTime analyzedAt;

    /**
     * 通知发送时间
     */
    private LocalDateTime notifiedAt;

    /**
     * 摘要信息
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * 告警 ID 列表（用于关联）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 错误信息
     */
    @Column(length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 检查时间窗口是否已结束
     */
    public boolean isWindowEnded() {
        return LocalDateTime.now().isAfter(windowEnd);
    }
}

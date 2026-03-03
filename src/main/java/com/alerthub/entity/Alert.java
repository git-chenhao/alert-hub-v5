package com.alerthub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 告警实体类
 */
@Entity
@Table(name = "alert", indexes = {
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_batch_id", columnList = "batchId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警指纹（用于去重）
     */
    @Column(nullable = false, length = 64, unique = true)
    private String fingerprint;

    /**
     * 告警来源
     */
    @Column(nullable = false, length = 100)
    private String source;

    /**
     * 告警标题
     */
    @Column(nullable = false, length = 500)
    private String title;

    /**
     * 告警内容
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 告警级别（critical, warning, info）
     */
    @Column(nullable = false, length = 20)
    private String severity;

    /**
     * 告警状态（pending, aggregated, sent, resolved）
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "pending";

    /**
     * 关联的批次 ID
     */
    @Column(name = "batch_id")
    private Long batchId;

    /**
     * 标签（JSON 格式）
     */
    @Column(columnDefinition = "TEXT")
    private String labels;

    /**
     * 注解（JSON 格式）
     */
    @Column(columnDefinition = "TEXT")
    private String annotations;

    /**
     * 发送次数
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer sendCount = 0;

    /**
     * 最后发送时间
     */
    private LocalDateTime lastSentAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 扩展字段（JSON 格式）
     */
    @Column(columnDefinition = "TEXT")
    private String extra;
}

package com.alerthub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_fingerprint", columnList = "fingerprint"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_batch_id", columnList = "batchId")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 告警唯一标识（外部系统） */
    @Column(length = 100)
    private String alertId;

    /** 指纹（用于去重） */
    @Column(length = 64, nullable = false)
    private String fingerprint;

    /** 告警名称 */
    @Column(length = 255, nullable = false)
    private String alertName;

    /** 告警来源 */
    @Column(length = 100, nullable = false)
    private String source;

    /** 严重程度：critical, high, medium, low, info */
    @Column(length = 20, nullable = false)
    private String severity;

    /** 告警状态：pending, aggregated, analyzing, resolved, suppressed */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "pending";

    /** 告警消息 */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 详细描述 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 标签（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String labels;

    /** 注解（JSON格式） */
    @Column(columnDefinition = "TEXT")
    private String annotations;

    /** 告警开始时间 */
    private LocalDateTime startsAt;

    /** 告警结束时间 */
    private LocalDateTime endsAt;

    /** 所属批次ID */
    private Long batchId;

    /** 根因分析结果 */
    @Column(columnDefinition = "TEXT")
    private String rootCauseAnalysis;

    /** 重试次数 */
    @Builder.Default
    private Integer retryCount = 0;

    /** 创建时间 */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 计算指纹
     */
    public static String calculateFingerprint(String source, String alertName, String severity, String labels) {
        String raw = String.format("%s|%s|%s|%s",
            source != null ? source : "",
            alertName != null ? alertName : "",
            severity != null ? severity : "",
            labels != null ? labels : ""
        );
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
}

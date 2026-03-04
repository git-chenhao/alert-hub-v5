package com.alerthub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 告警批次实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "alert_batches", indexes = {
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_created_at", columnList = "createdAt")
})
public class AlertBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 批次编号 */
    @Column(unique = true, nullable = false)
    private String batchNo;

    /** 批次状态：open, processing, completed, failed */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "open";

    /** 告警数量 */
    @Builder.Default
    private Integer alertCount = 0;

    /** 聚合窗口开始时间 */
    private LocalDateTime windowStart;

    /** 聚合窗口结束时间 */
    private LocalDateTime windowEnd;

    /** 根因分析结果 */
    @Column(columnDefinition = "TEXT")
    private String analysisResult;

    /** 飞书消息ID */
    @Column(length = 100)
    private String feishuMessageId;

    /** 处理时间 */
    private LocalDateTime processedAt;

    /** 创建时间 */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** 更新时间 */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * 生成批次编号
     */
    public static String generateBatchNo() {
        return String.format("BATCH-%s-%04d",
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")),
            (int)(Math.random() * 10000)
        );
    }
}

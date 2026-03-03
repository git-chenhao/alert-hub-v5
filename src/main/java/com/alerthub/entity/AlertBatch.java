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
 * 告警批次实体类
 */
@Entity
@Table(name = "alert_batch", indexes = {
    @Index(name = "idx_batch_status", columnList = "status"),
    @Index(name = "idx_batch_created_at", columnList = "createdAt"),
    @Index(name = "idx_window_start", columnList = "windowStart")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 批次号
     */
    @Column(nullable = false, length = 50, unique = true)
    private String batchNo;

    /**
     * 时间窗口开始时间
     */
    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    /**
     * 时间窗口结束时间
     */
    @Column(name = "window_end", nullable = false)
    private LocalDateTime windowEnd;

    /**
     * 批次状态（open, closed, sent）
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "open";

    /**
     * 告警数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer alertCount = 0;

    /**
     * 批次摘要
     */
    @Column(columnDefinition = "TEXT")
    private String summary;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 通知结果
     */
    @Column(columnDefinition = "TEXT")
    private String notificationResult;

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
}

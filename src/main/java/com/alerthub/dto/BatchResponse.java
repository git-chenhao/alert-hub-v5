package com.alerthub.dto;

import com.alerthub.model.AlertSeverity;
import com.alerthub.model.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 批次响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {

    /**
     * 批次 ID
     */
    private Long id;

    /**
     * 批次标识
     */
    private String batchKey;

    /**
     * 告警来源
     */
    private String source;

    /**
     * 严重级别
     */
    private AlertSeverity severity;

    /**
     * 时间窗口开始
     */
    private LocalDateTime windowStart;

    /**
     * 时间窗口结束
     */
    private LocalDateTime windowEnd;

    /**
     * 告警数量
     */
    private int alertCount;

    /**
     * 分析结果
     */
    private String analysisResult;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 状态
     */
    private BatchStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 分析完成时间
     */
    private LocalDateTime analyzedAt;

    /**
     * 通知发送时间
     */
    private LocalDateTime notifiedAt;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;
}

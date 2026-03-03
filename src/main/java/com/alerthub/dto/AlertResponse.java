package com.alerthub.dto;

import com.alerthub.model.AlertSeverity;
import com.alerthub.model.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    /**
     * 告警 ID
     */
    private Long id;

    /**
     * 告警指纹
     */
    private String fingerprint;

    /**
     * 告警来源
     */
    private String source;

    /**
     * 严重级别
     */
    private AlertSeverity severity;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警描述
     */
    private String description;

    /**
     * 标签
     */
    private Map<String, String> labels;

    /**
     * 状态
     */
    private AlertStatus status;

    /**
     * 接收时间
     */
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
     * 是否为重复告警
     */
    private boolean duplicate;

    /**
     * 消息
     */
    private String message;
}

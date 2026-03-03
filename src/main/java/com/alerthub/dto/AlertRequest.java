package com.alerthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 告警接收请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {

    /**
     * 告警来源（如 prometheus, grafana, zabbix 等）
     */
    @NotBlank(message = "告警来源不能为空")
    @Size(max = 100, message = "告警来源长度不能超过100字符")
    private String source;

    /**
     * 严重级别 (CRITICAL/HIGH/MEDIUM/LOW/INFO)
     */
    @NotBlank(message = "严重级别不能为空")
    private String severity;

    /**
     * 告警标题
     */
    @NotBlank(message = "告警标题不能为空")
    @Size(max = 500, message = "告警标题长度不能超过500字符")
    private String title;

    /**
     * 告警描述
     */
    private String description;

    /**
     * 标签（用于分组和过滤）
     */
    private Map<String, String> labels;

    /**
     * 原始数据（可选，用于记录原始告警内容）
     */
    private String rawPayload;

    /**
     * 时间戳（ISO 8601 格式，可选）
     */
    private String timestamp;
}

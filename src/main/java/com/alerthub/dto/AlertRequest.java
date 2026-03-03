package com.alerthub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 告警请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequest {

    /**
     * 告警来源
     */
    @NotBlank(message = "告警来源不能为空")
    private String source;

    /**
     * 告警标题
     */
    @NotBlank(message = "告警标题不能为空")
    private String title;

    /**
     * 告警内容
     */
    private String content;

    /**
     * 告警级别（critical, warning, info）
     */
    @NotBlank(message = "告警级别不能为空")
    private String severity;

    /**
     * 标签
     */
    private Map<String, String> labels;

    /**
     * 注解
     */
    private Map<String, String> annotations;

    /**
     * 扩展字段
     */
    private Map<String, Object> extra;
}

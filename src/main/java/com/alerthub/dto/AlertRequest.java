package com.alerthub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRequest {

    @NotBlank(message = "告警名称不能为空")
    private String alertName;

    @NotBlank(message = "告警来源不能为空")
    private String source;

    @NotBlank(message = "严重程度不能为空")
    private String severity;

    private String message;

    private String description;

    private Map<String, String> labels;

    private Map<String, String> annotations;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    /**
     * 验证严重程度
     */
    public boolean isValidSeverity() {
        return severity != null &&
            (severity.equalsIgnoreCase("critical") ||
             severity.equalsIgnoreCase("high") ||
             severity.equalsIgnoreCase("medium") ||
             severity.equalsIgnoreCase("low") ||
             severity.equalsIgnoreCase("info"));
    }
}

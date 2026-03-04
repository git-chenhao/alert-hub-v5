package com.alerthub.a2a;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * A2A 协议响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class A2AResponse {

    /** 任务 ID */
    private String taskId;

    /** 任务状态 */
    private String status;

    /** 根因分析结果 */
    private AnalysisResult result;

    /** 错误信息 */
    private String error;

    /** 处理时间 */
    private LocalDateTime processedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AnalysisResult {
        /** 根因类型 */
        private String rootCauseType;

        /** 根因描述 */
        private String rootCauseDescription;

        /** 置信度 (0-100) */
        private Integer confidence;

        /** 建议操作 */
        private List<String> recommendations;

        /** 相关告警 */
        private List<String> relatedAlerts;

        /** 详细分析 */
        private Map<String, Object> details;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "completed".equals(status) || "success".equals(status);
    }
}

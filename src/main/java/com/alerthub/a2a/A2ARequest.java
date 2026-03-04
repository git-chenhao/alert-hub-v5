package com.alerthub.a2a;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * A2A 协议请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class A2ARequest {

    /** 任务 ID */
    private String taskId;

    /** 任务类型 */
    private String taskType;

    /** 告警数据 */
    private AlertData alert;

    /** 上下文信息 */
    private Map<String, Object> context;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertData {
        private Long id;
        private String fingerprint;
        private String alertName;
        private String source;
        private String severity;
        private String message;
        private String description;
        private Map<String, String> labels;
        private Map<String, String> annotations;
        private String batchNo;
    }

    /**
     * 创建根因分析请求
     */
    public static A2ARequest forRootCauseAnalysis(com.alerthub.entity.Alert alert, String batchNo) {
        return A2ARequest.builder()
            .taskId(java.util.UUID.randomUUID().toString())
            .taskType("root_cause_analysis")
            .alert(AlertData.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .alertName(alert.getAlertName())
                .source(alert.getSource())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .description(alert.getDescription())
                .batchNo(batchNo)
                .build())
            .context(Map.of(
                "timestamp", java.time.LocalDateTime.now().toString(),
                "platform", "alert-hub-v5"
            ))
            .build();
    }

    /**
     * 创建批量根因分析请求
     */
    public static A2ARequest forBatchRootCauseAnalysis(List<com.alerthub.entity.Alert> alerts, String batchNo) {
        java.util.List<AlertData> alertDataList = alerts.stream()
            .map(alert -> AlertData.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .alertName(alert.getAlertName())
                .source(alert.getSource())
                .severity(alert.getSeverity())
                .message(alert.getMessage())
                .description(alert.getDescription())
                .batchNo(batchNo)
                .build())
            .toList();

        return A2ARequest.builder()
            .taskId(java.util.UUID.randomUUID().toString())
            .taskType("batch_root_cause_analysis")
            .alert(alertDataList.get(0)) // 主告警
            .context(Map.of(
                "timestamp", java.time.LocalDateTime.now().toString(),
                "platform", "alert-hub-v5",
                "alertCount", alerts.size(),
                "alerts", alertDataList
            ))
            .build();
    }
}

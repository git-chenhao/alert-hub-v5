package com.alerthub.service;

import com.alerthub.a2a.A2AResponse;
import com.alerthub.config.AlertHubProperties;
import com.alerthub.dto.AlertReceivedData;
import com.alerthub.dto.AlertRequest;
import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.alerthub.repository.AlertBatchRepository;
import com.alerthub.repository.AlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 告警核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository alertBatchRepository;
    private final DeduplicationService deduplicationService;
    private final AggregationService aggregationService;
    private final A2AService a2aService;
    private final FeishuService feishuService;
    private final AlertHubProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 接收告警
     */
    @Transactional
    public AlertReceivedData receiveAlert(AlertRequest request) {
        // 验证严重程度
        if (!request.isValidSeverity()) {
            throw new IllegalArgumentException("无效的严重程度: " + request.getSeverity());
        }

        // 计算指纹
        String fingerprint = deduplicationService.calculateFingerprint(request);

        // 检查去重
        if (deduplicationService.isDuplicate(request)) {
            log.info("告警被去重: fingerprint={}", fingerprint);
            return AlertReceivedData.duplicate(fingerprint);
        }

        // 记录指纹
        deduplicationService.recordFingerprint(request);

        // 创建告警实体
        Alert alert = createAlertEntity(request, fingerprint);
        alert = alertRepository.save(alert);

        // 加入聚合批次
        aggregationService.addToBatch(alert);

        log.info("告警接收成功: id={}, fingerprint={}", alert.getId(), fingerprint);
        return AlertReceivedData.accepted(alert.getId(), fingerprint);
    }

    /**
     * 创建告警实体
     */
    private Alert createAlertEntity(AlertRequest request, String fingerprint) {
        return Alert.builder()
            .alertId(java.util.UUID.randomUUID().toString())
            .fingerprint(fingerprint)
            .alertName(request.getAlertName())
            .source(request.getSource())
            .severity(request.getSeverity().toLowerCase())
            .status("pending")
            .message(request.getMessage())
            .description(request.getDescription())
            .labels(serializeMap(request.getLabels()))
            .annotations(serializeMap(request.getAnnotations()))
            .startsAt(request.getStartsAt() != null ? request.getStartsAt() : LocalDateTime.now())
            .endsAt(request.getEndsAt())
            .retryCount(0)
            .build();
    }

    /**
     * 处理批次（聚合完成后调用）
     */
    @Transactional
    public void processBatchAnalysis(Long batchId) {
        AlertBatch batch = alertBatchRepository.findById(batchId)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + batchId));

        List<Alert> alerts = alertRepository.findByBatchId(batchId);

        if (alerts.isEmpty()) {
            log.warn("批次没有告警: batchId={}", batchId);
            return;
        }

        // 调用 A2A 进行根因分析
        A2AResponse analysisResult = a2aService.analyzeBatchRootCause(alerts, batch.getBatchNo());

        // 保存分析结果
        if (analysisResult.isSuccess() && analysisResult.getResult() != null) {
            try {
                batch.setAnalysisResult(objectMapper.writeValueAsString(analysisResult.getResult()));
            } catch (JsonProcessingException e) {
                log.error("序列化分析结果失败", e);
            }
        }

        // 批量更新告警状态和根因分析结果（避免 N+1 问题）
        List<Long> alertIds = alerts.stream().map(Alert::getId).toList();
        String rootCauseDescription = analysisResult.getResult() != null ?
            analysisResult.getResult().getRootCauseDescription() : null;
        alertRepository.updateStatusAndRootCauseByIds("analyzed", rootCauseDescription, alertIds);

        alertBatchRepository.save(batch);

        // 发送飞书通知
        feishuService.sendBatchNotification(batch, alerts, analysisResult);
    }

    /**
     * 获取告警详情
     */
    public Optional<Alert> getAlert(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * 获取告警列表
     */
    public Page<Alert> getAlerts(String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return alertRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        }
        return alertRepository.findAll(pageable);
    }

    /**
     * 获取批次列表
     */
    public Page<AlertBatch> getBatches(Pageable pageable) {
        return alertBatchRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /**
     * 获取批次详情
     */
    public Optional<AlertBatch> getBatch(Long id) {
        return alertBatchRepository.findById(id);
    }

    /**
     * 获取批次内的告警
     */
    public List<Alert> getBatchAlerts(Long batchId) {
        return alertRepository.findByBatchId(batchId);
    }

    /**
     * 更新告警状态
     */
    @Transactional
    public Alert updateAlertStatus(Long id, String status) {
        Alert alert = alertRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + id));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }

    /**
     * 手动触发根因分析
     */
    @Transactional
    public A2AResponse triggerRootCauseAnalysis(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + alertId));

        String batchNo = alert.getBatchId() != null ?
            alertBatchRepository.findById(alert.getBatchId()).map(AlertBatch::getBatchNo).orElse(null) : null;

        A2AResponse response = a2aService.analyzeRootCause(alert, batchNo);

        if (response.isSuccess() && response.getResult() != null) {
            alert.setRootCauseAnalysis(response.getResult().getRootCauseDescription());
            alert.setStatus("analyzed");
            alertRepository.save(alert);
        }

        return response;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();

        stats.put("alertsByStatus", alertRepository.countBySeverity());
        stats.put("alertsBySource", alertRepository.countBySource());

        AggregationService.BatchStatistics batchStats = aggregationService.getStatistics();
        stats.put("pendingAlerts", batchStats.pendingAlerts());
        stats.put("aggregatedAlerts", batchStats.aggregatedAlerts());
        stats.put("analyzingAlerts", batchStats.analyzingAlerts());
        stats.put("resolvedAlerts", batchStats.resolvedAlerts());
        stats.put("suppressedAlerts", batchStats.suppressedAlerts());
        stats.put("openBatches", batchStats.openBatches());
        stats.put("processingBatches", batchStats.processingBatches());
        stats.put("completedBatches", batchStats.completedBatches());

        return stats;
    }

    /**
     * 序列化 Map
     */
    private String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("序列化 Map 失败", e);
            return null;
        }
    }
}

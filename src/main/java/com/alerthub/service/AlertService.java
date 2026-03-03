package com.alerthub.service;

import com.alerthub.dto.AlertRequest;
import com.alerthub.dto.AlertResponse;
import com.alerthub.model.Alert;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.AlertStatus;
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
    private final DeduplicationService deduplicationService;
    private final AggregationService aggregationService;
    private final ObjectMapper objectMapper;

    /**
     * 接收并处理告警
     */
    @Transactional
    public AlertResponse receiveAlert(AlertRequest request) {
        log.info("Receiving alert from source: {}, title: {}", request.getSource(), request.getTitle());

        // 解析严重级别
        AlertSeverity severity = AlertSeverity.fromString(request.getSeverity());

        // 检查是否为重复告警
        String fingerprint = deduplicationService.generateFingerprint(
                request.getSource(),
                request.getSeverity(),
                request.getTitle(),
                request.getLabels()
        );

        boolean isDuplicate = deduplicationService.isDuplicateByFingerprint(fingerprint);

        // 构建告警实体
        Alert alert = Alert.builder()
                .fingerprint(fingerprint)
                .source(request.getSource())
                .severity(severity)
                .title(request.getTitle())
                .description(request.getDescription())
                .labels(request.getLabels() != null ? request.getLabels() : Map.of())
                .rawPayload(serializeRawPayload(request))
                .status(isDuplicate ? AlertStatus.DUPLICATED : AlertStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .build();

        // 保存告警
        alert = alertRepository.save(alert);
        log.info("Alert saved with id: {}, fingerprint: {}, duplicate: {}",
                alert.getId(), alert.getFingerprint(), isDuplicate);

        // 如果不是重复告警，记录指纹并加入聚合
        if (!isDuplicate) {
            deduplicationService.recordFingerprint(fingerprint);
            aggregationService.addAlertToBatch(alert);
        }

        return buildAlertResponse(alert, isDuplicate);
    }

    /**
     * 根据 ID 查询告警
     */
    public Optional<Alert> findById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * 根据指纹查询告警
     */
    public Optional<Alert> findByFingerprint(String fingerprint) {
        return alertRepository.findByFingerprint(fingerprint);
    }

    /**
     * 分页查询告警
     */
    public Page<Alert> findAll(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    /**
     * 条件查询告警
     */
    public Page<Alert> searchAlerts(String source, AlertSeverity severity, AlertStatus status,
                                     LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return alertRepository.searchAlerts(source, severity, status, startTime, endTime, pageable);
    }

    /**
     * 更新告警状态
     */
    @Transactional
    public void updateStatus(Long alertId, AlertStatus status) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setStatus(status);
            alert.setProcessedAt(LocalDateTime.now());
            alertRepository.save(alert);
        });
    }

    /**
     * 分配告警到批次
     */
    @Transactional
    public void assignToBatch(Long alertId, Long batchId) {
        alertRepository.findById(alertId).ifPresent(alert -> {
            alert.setBatchId(batchId);
            alert.setStatus(AlertStatus.AGGREGATED);
            alert.setProcessedAt(LocalDateTime.now());
            alertRepository.save(alert);
        });
    }

    /**
     * 查询未分配批次的告警
     */
    public List<Alert> findUnbatchedAlerts() {
        return alertRepository.findUnbatchedAlerts(AlertStatus.RECEIVED);
    }

    /**
     * 统计告警数量
     */
    public long count() {
        return alertRepository.count();
    }

    /**
     * 统计指定状态告警数量
     */
    public long countByStatus(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }

    /**
     * 清理过期告警
     */
    @Transactional
    public int cleanupOldAlerts(int retentionDays) {
        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        alertRepository.deleteByReceivedAtBefore(before);
        log.info("Cleaned up alerts older than {} days", retentionDays);
        return 0; // JPA delete doesn't return count easily
    }

    /**
     * 序列化原始数据
     */
    private String serializeRawPayload(AlertRequest request) {
        if (request.getRawPayload() != null) {
            return request.getRawPayload();
        }
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize raw payload: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 构建响应
     */
    private AlertResponse buildAlertResponse(Alert alert, boolean isDuplicate) {
        return AlertResponse.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .source(alert.getSource())
                .severity(alert.getSeverity())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .labels(alert.getLabels())
                .status(alert.getStatus())
                .receivedAt(alert.getReceivedAt())
                .processedAt(alert.getProcessedAt())
                .batchId(alert.getBatchId())
                .duplicate(isDuplicate)
                .message(isDuplicate ? "Duplicate alert detected" : "Alert received successfully")
                .build();
    }
}

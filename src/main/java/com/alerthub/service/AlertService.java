package com.alerthub.service;

import com.alerthub.dto.AlertRequest;
import com.alerthub.entity.Alert;
import com.alerthub.repository.AlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 告警服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;
    private final DeduplicationService deduplicationService;

    /**
     * 接收告警
     */
    @Transactional
    public Alert receiveAlert(AlertRequest request) {
        log.info("接收告警: source={}, title={}, severity={}",
            request.getSource(), request.getTitle(), request.getSeverity());

        // 生成指纹
        String fingerprint = generateFingerprint(request);
        log.debug("生成指纹: {}", fingerprint);

        // 检查是否重复
        if (deduplicationService.isDuplicate(fingerprint)) {
            log.warn("告警重复，已去重: fingerprint={}", fingerprint);
            return getExistingAlert(fingerprint).orElse(null);
        }

        // 创建告警
        Alert alert = Alert.builder()
            .fingerprint(fingerprint)
            .source(request.getSource())
            .title(request.getTitle())
            .content(request.getContent())
            .severity(request.getSeverity())
            .status("pending")
            .labels(toJson(request.getLabels()))
            .annotations(toJson(request.getAnnotations()))
            .extra(toJson(request.getExtra()))
            .build();

        Alert saved = alertRepository.save(alert);
        log.info("告警已保存: id={}", saved.getId());

        return saved;
    }

    /**
     * 生成告警指纹
     */
    public String generateFingerprint(AlertRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getSource())
          .append("|")
          .append(request.getTitle())
          .append("|")
          .append(request.getSeverity());

        // 如果有标签，添加到指纹中
        if (request.getLabels() != null && !request.getLabels().isEmpty()) {
            sb.append("|");
            request.getLabels().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(e.getKey()).append("=").append(e.getValue()).append(";"));
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("生成指纹失败", e);
            return sb.toString().hashCode() + "";
        }
    }

    /**
     * 获取现有告警
     */
    public Optional<Alert> getExistingAlert(String fingerprint) {
        return alertRepository.findByFingerprint(fingerprint);
    }

    /**
     * 获取所有告警（分页）
     */
    public Page<Alert> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    /**
     * 根据ID获取告警
     */
    public Optional<Alert> findById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * 根据状态获取告警
     */
    public Page<Alert> getAlertsByStatus(String status, Pageable pageable) {
        return alertRepository.findByStatus(status, pageable);
    }

    /**
     * 获取待处理告警
     */
    public List<Alert> getPendingAlerts() {
        return alertRepository.findPendingAlertsAfter(LocalDateTime.now().minusHours(1));
    }

    /**
     * 更新告警状态
     */
    @Transactional
    public Alert updateAlertStatus(Long id, String status) {
        Alert alert = alertRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("告警不存在: " + id));
        alert.setStatus(status);
        return alertRepository.save(alert);
    }

    /**
     * 批量更新告警状态（使用批量更新避免 N+1 查询）
     */
    @Transactional
    public void batchUpdateStatus(List<Long> ids, String status, Long batchId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        alertRepository.batchUpdateStatusAndBatchId(ids, status, batchId);
    }

    /**
     * 统计告警数量
     */
    public long countAlerts() {
        return alertRepository.count();
    }

    /**
     * 统计待处理告警数量
     */
    public long countPendingAlerts() {
        return alertRepository.countByStatus("pending");
    }

    /**
     * 获取最近告警
     */
    public List<Alert> getRecentAlerts() {
        return alertRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 对象转 JSON
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }
}

package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.dto.AlertRequest;
import com.alerthub.entity.DeduplicationRecord;
import com.alerthub.repository.DeduplicationRecordRepository;
import com.alerthub.util.FingerprintUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 去重服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final DeduplicationRecordRepository deduplicationRecordRepository;
    private final AlertHubProperties properties;

    /**
     * 检查告警是否重复
     * @return true 表示是重复告警
     */
    @Transactional
    public boolean isDuplicate(AlertRequest request) {
        if (!properties.getDeduplication().isEnabled()) {
            return false;
        }

        String fingerprint = calculateFingerprint(request);

        return deduplicationRecordRepository.findByFingerprint(fingerprint)
            .map(record -> {
                // 检查是否过期
                if (record.getExpiresAt() != null && record.getExpiresAt().isBefore(LocalDateTime.now())) {
                    // 已过期，重新记录
                    deduplicationRecordRepository.delete(record);
                    return false;
                }
                // 更新最后出现时间和计数
                deduplicationRecordRepository.updateLastSeen(fingerprint, LocalDateTime.now());
                log.debug("告警重复: fingerprint={}, count={}", fingerprint, record.getOccurrenceCount() + 1);
                return true;
            })
            .orElse(false);
    }

    /**
     * 记录告警指纹
     */
    @Transactional
    public void recordFingerprint(AlertRequest request) {
        if (!properties.getDeduplication().isEnabled()) {
            return;
        }

        String fingerprint = calculateFingerprint(request);
        int ttlHours = properties.getDeduplication().getTtlHours();

        DeduplicationRecord record = DeduplicationRecord.builder()
            .fingerprint(fingerprint)
            .lastSeenAt(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(ttlHours))
            .occurrenceCount(1)
            .build();

        try {
            deduplicationRecordRepository.save(record);
            log.debug("记录告警指纹: fingerprint={}", fingerprint);
        } catch (Exception e) {
            // 可能是并发插入导致的唯一键冲突，忽略
            log.debug("指纹已存在: fingerprint={}", fingerprint);
        }
    }

    /**
     * 计算告警指纹
     */
    public String calculateFingerprint(AlertRequest request) {
        String algorithm = properties.getDeduplication().getAlgorithm();
        return FingerprintUtil.calculateAlertFingerprint(
            request.getSource(),
            request.getAlertName(),
            request.getSeverity(),
            request.getLabels(),
            algorithm
        );
    }

    /**
     * 手动计算指纹（用于测试）
     */
    public String calculateFingerprint(String source, String alertName, String severity, Map<String, String> labels) {
        String algorithm = properties.getDeduplication().getAlgorithm();
        return FingerprintUtil.calculateAlertFingerprint(source, alertName, severity, labels, algorithm);
    }

    /**
     * 清理过期的去重记录（每小时执行）
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupExpiredRecords() {
        int deleted = deduplicationRecordRepository.deleteExpiredRecords(LocalDateTime.now());
        if (deleted > 0) {
            log.info("清理过期去重记录: {} 条", deleted);
        }
    }
}

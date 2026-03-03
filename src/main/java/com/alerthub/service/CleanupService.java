package com.alerthub.service;

import com.alerthub.repository.AlertBatchRepository;
import com.alerthub.repository.AlertRepository;
import com.alerthub.service.DeduplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 定时清理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CleanupService {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository batchRepository;
    private final DeduplicationService deduplicationService;

    /**
     * 告警数据保留天数
     */
    @Value("${alerthub.retention.alert-days:30}")
    private int alertRetentionDays;

    /**
     * 批次数据保留天数
     */
    @Value("${alerthub.retention.batch-days:90}")
    private int batchRetentionDays;

    /**
     * 清理过期告警
     * 每天凌晨 2 点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldAlerts() {
        log.info("Starting cleanup of old alerts (retention: {} days)", alertRetentionDays);

        LocalDateTime before = LocalDateTime.now().minusDays(alertRetentionDays);
        alertRepository.deleteByReceivedAtBefore(before);

        log.info("Completed cleanup of alerts older than {}", before);
    }

    /**
     * 清理过期批次
     * 每天凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldBatches() {
        log.info("Starting cleanup of old batches (retention: {} days)", batchRetentionDays);

        LocalDateTime before = LocalDateTime.now().minusDays(batchRetentionDays);
        batchRepository.deleteByCreatedAtBefore(before);

        log.info("Completed cleanup of batches older than {}", before);
    }

    /**
     * 清理去重缓存
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupDeduplicationCache() {
        log.debug("Starting cleanup of deduplication cache");
        deduplicationService.cleanupExpiredCache();
        log.debug("Completed cleanup of deduplication cache, cache size: {}",
                deduplicationService.getCacheSize());
    }
}

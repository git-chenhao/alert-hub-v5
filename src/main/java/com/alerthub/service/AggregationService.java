package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.model.Alert;
import com.alerthub.model.AlertBatch;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.BatchStatus;
import com.alerthub.repository.AlertBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警聚合服务
 * 按时间窗口和 source + severity 分组聚合告警
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final AlertBatchRepository batchRepository;
    private final AlertService alertService;
    private final AlertHubProperties properties;

    /**
     * 将告警添加到批次
     */
    @Transactional
    public void addAlertToBatch(Alert alert) {
        if (!properties.getAggregation().isEnabled()) {
            log.debug("Aggregation is disabled, skipping batch assignment");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int windowMinutes = properties.getAggregation().getWindowMinutes();

        // 查找或创建活跃批次
        Optional<AlertBatch> activeBatch = batchRepository.findActiveBatch(
                alert.getSource(),
                alert.getSeverity(),
                now
        );

        AlertBatch batch;
        if (activeBatch.isPresent()) {
            batch = activeBatch.get();
            // 增加告警计数
            batchRepository.incrementAlertCount(batch.getId(), now);
            log.debug("Added alert {} to existing batch {}", alert.getId(), batch.getId());
        } else {
            // 创建新批次
            batch = createNewBatch(alert.getSource(), alert.getSeverity(), now, windowMinutes);
            batch = batchRepository.save(batch);
            log.info("Created new batch {} for source: {}, severity: {}",
                    batch.getId(), alert.getSource(), alert.getSeverity());
        }

        // 更新告警的批次 ID
        alertService.assignToBatch(alert.getId(), batch.getId());
    }

    /**
     * 创建新批次
     */
    private AlertBatch createNewBatch(String source, AlertSeverity severity,
                                       LocalDateTime startTime, int windowMinutes) {
        LocalDateTime windowEnd = startTime.plusMinutes(windowMinutes);
        String batchKey = generateBatchKey(source, severity, startTime, windowEnd);

        return AlertBatch.builder()
                .batchKey(batchKey)
                .source(source)
                .severity(severity)
                .windowStart(startTime)
                .windowEnd(windowEnd)
                .alertCount(1)
                .status(BatchStatus.AGGREGATING)
                .createdAt(startTime)
                .build();
    }

    /**
     * 生成批次标识
     */
    private String generateBatchKey(String source, AlertSeverity severity,
                                     LocalDateTime windowStart, LocalDateTime windowEnd) {
        return String.format("%s_%s_%s_%s",
                source,
                severity.name(),
                windowStart.toString().replace(":", "-"),
                windowEnd.toString().replace(":", "-")
        ).toLowerCase();
    }

    /**
     * 定时处理已结束时间窗口的批次
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processCompletedBatches() {
        if (!properties.getAggregation().isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<AlertBatch> processableBatches = batchRepository.findProcessableBatches(
                BatchStatus.AGGREGATING, now
        );

        log.debug("Found {} batches ready for processing", processableBatches.size());

        for (AlertBatch batch : processableBatches) {
            try {
                processBatch(batch);
            } catch (Exception e) {
                log.error("Failed to process batch {}: {}", batch.getId(), e.getMessage(), e);
                batch.setStatus(BatchStatus.FAILED);
                batch.setErrorMessage(e.getMessage());
                batchRepository.save(batch);
            }
        }
    }

    /**
     * 处理单个批次
     */
    private void processBatch(AlertBatch batch) {
        log.info("Processing batch {} with {} alerts", batch.getId(), batch.getAlertCount());

        // 生成摘要
        generateBatchSummary(batch);

        // 更新状态为待分析
        batch.setStatus(BatchStatus.PENDING_ANALYSIS);
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);

        log.info("Batch {} processed, status changed to PENDING_ANALYSIS", batch.getId());
    }

    /**
     * 生成批次摘要
     */
    private void generateBatchSummary(AlertBatch batch) {
        StringBuilder summary = new StringBuilder();
        summary.append("告警批次摘要:\n");
        summary.append(String.format("- 来源: %s\n", batch.getSource()));
        summary.append(String.format("- 严重级别: %s\n", batch.getSeverity()));
        summary.append(String.format("- 时间窗口: %s 至 %s\n",
                batch.getWindowStart(), batch.getWindowEnd()));
        summary.append(String.format("- 告警数量: %d", batch.getAlertCount()));

        batch.setSummary(summary.toString());
    }

    /**
     * 查询活跃批次
     */
    public List<AlertBatch> findActiveBatches() {
        return batchRepository.findActiveBatches(LocalDateTime.now());
    }

    /**
     * 查询待分析批次
     */
    public List<AlertBatch> findPendingAnalysisBatches() {
        return batchRepository.findPendingAnalysisBatches();
    }

    /**
     * 查询待通知批次
     */
    public List<AlertBatch> findPendingNotificationBatches() {
        return batchRepository.findPendingNotificationBatches();
    }

    /**
     * 更新批次状态
     */
    @Transactional
    public void updateBatchStatus(Long batchId, BatchStatus status) {
        batchRepository.updateStatus(batchId, status, LocalDateTime.now());
    }
}

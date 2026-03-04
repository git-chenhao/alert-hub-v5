package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.alerthub.repository.AlertBatchRepository;
import com.alerthub.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聚合策略服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository alertBatchRepository;
    private final AlertHubProperties properties;

    /**
     * 将告警加入当前批次
     */
    @Transactional
    public void addToBatch(Alert alert) {
        if (!properties.getAggregation().isEnabled()) {
            // 不聚合，直接处理
            alert.setStatus("pending_analysis");
            alertRepository.save(alert);
            return;
        }

        // 查找或创建当前批次
        AlertBatch currentBatch = getOrCreateCurrentBatch();

        // 将告警加入批次
        alert.setBatchId(currentBatch.getId());
        alert.setStatus("aggregated");
        alertRepository.save(alert);

        // 更新批次告警数量
        currentBatch.setAlertCount(currentBatch.getAlertCount() + 1);
        alertBatchRepository.save(currentBatch);

        log.debug("告警加入批次: alertId={}, batchId={}", alert.getId(), currentBatch.getId());

        // 检查是否达到最大批次大小
        if (currentBatch.getAlertCount() >= properties.getAggregation().getMaxBatchSize()) {
            processBatch(currentBatch);
        }
    }

    /**
     * 获取或创建当前批次
     */
    @Transactional
    public AlertBatch getOrCreateCurrentBatch() {
        int windowSeconds = properties.getAggregation().getWindowSeconds();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusSeconds(windowSeconds);

        // 查找状态为 open 且窗口未结束的批次
        List<AlertBatch> openBatches = alertBatchRepository.findByStatus("open");

        for (AlertBatch batch : openBatches) {
            if (batch.getWindowEnd() != null && batch.getWindowEnd().isAfter(now)) {
                return batch;
            }
        }

        // 创建新批次
        AlertBatch newBatch = AlertBatch.builder()
            .batchNo(AlertBatch.generateBatchNo())
            .status("open")
            .alertCount(0)
            .windowStart(now)
            .windowEnd(windowEnd)
            .build();

        return alertBatchRepository.save(newBatch);
    }

    /**
     * 定时检查并处理就绪的批次（每 10 秒执行）
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void checkAndProcessReadyBatches() {
        if (!properties.getAggregation().isEnabled()) {
            return;
        }

        List<AlertBatch> readyBatches = alertBatchRepository.findReadyToProcessBatches(LocalDateTime.now());

        for (AlertBatch batch : readyBatches) {
            try {
                processBatch(batch);
            } catch (Exception e) {
                log.error("处理批次失败: batchNo={}", batch.getBatchNo(), e);
                batch.setStatus("failed");
                alertBatchRepository.save(batch);
            }
        }
    }

    /**
     * 处理批次
     */
    @Transactional
    public void processBatch(AlertBatch batch) {
        log.info("开始处理批次: batchNo={}, alertCount={}", batch.getBatchNo(), batch.getAlertCount());

        batch.setStatus("processing");
        alertBatchRepository.save(batch);

        // 获取批次内所有告警
        List<Alert> alerts = alertRepository.findByBatchId(batch.getId());

        if (alerts.isEmpty()) {
            batch.setStatus("completed");
            batch.setProcessedAt(LocalDateTime.now());
            alertBatchRepository.save(batch);
            return;
        }

        // 更新告警状态
        alerts.forEach(alert -> {
            alert.setStatus("analyzing");
            alertRepository.save(alert);
        });

        batch.setStatus("completed");
        batch.setProcessedAt(LocalDateTime.now());
        alertBatchRepository.save(batch);

        log.info("批次处理完成: batchNo={}", batch.getBatchNo());
    }

    /**
     * 获取批次统计信息
     */
    public BatchStatistics getStatistics() {
        long pending = alertRepository.countByStatus("pending");
        long aggregated = alertRepository.countByStatus("aggregated");
        long analyzing = alertRepository.countByStatus("analyzing");
        long resolved = alertRepository.countByStatus("resolved");
        long suppressed = alertRepository.countByStatus("suppressed");

        long openBatches = alertBatchRepository.countByStatus("open");
        long processingBatches = alertBatchRepository.countByStatus("processing");
        long completedBatches = alertBatchRepository.countByStatus("completed");

        return new BatchStatistics(
            pending, aggregated, analyzing, resolved, suppressed,
            openBatches, processingBatches, completedBatches
        );
    }

    /**
     * 批次统计信息
     */
    public record BatchStatistics(
        long pendingAlerts,
        long aggregatedAlerts,
        long analyzingAlerts,
        long resolvedAlerts,
        long suppressedAlerts,
        long openBatches,
        long processingBatches,
        long completedBatches
    ) {}
}

package com.alerthub.service;

import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.alerthub.repository.AlertBatchRepository;
import com.alerthub.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 告警聚合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AggregationService {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository batchRepository;
    private final FeishuNotificationService notificationService;

    @Value("${alerthub.aggregation.enabled:true}")
    private boolean enabled;

    @Value("${alerthub.aggregation.window-size:60}")
    private int windowSize;

    @Value("${alerthub.aggregation.max-batch-size:100}")
    private int maxBatchSize;

    private static final DateTimeFormatter BATCH_NO_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 定时聚合任务 - 每分钟执行
     * 使用 fixedDelay 避免任务重叠执行
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void aggregateAlerts() {
        if (!enabled) {
            return;
        }

        log.debug("开始告警聚合任务");

        // 获取待处理告警（带数量限制，防止内存溢出）
        List<Alert> pendingAlerts = alertRepository.findPendingAlertsAfterWithLimit(
            LocalDateTime.now().minusMinutes(windowSize),
            maxBatchSize
        );

        if (pendingAlerts.isEmpty()) {
            log.debug("没有待处理的告警");
            return;
        }

        log.info("发现 {} 条待处理告警", pendingAlerts.size());

        // 创建批次
        AlertBatch batch = createBatch(pendingAlerts);

        // 更新告警状态
        List<Long> alertIds = pendingAlerts.stream()
            .map(Alert::getId)
            .toList();
        updateAlertsStatus(alertIds, batch.getId());

        // 发送通知
        notificationService.sendBatchNotification(batch, pendingAlerts);

        // 更新批次状态为已发送
        batch.setStatus("sent");
        batch.setSentAt(LocalDateTime.now());
        batchRepository.save(batch);

        log.info("告警聚合完成: batchNo={}, alertCount={}", batch.getBatchNo(), batch.getAlertCount());
    }

    /**
     * 创建告警批次
     */
    private AlertBatch createBatch(List<Alert> alerts) {
        String batchNo = "BATCH-" + LocalDateTime.now().format(BATCH_NO_FORMATTER) +
            "-" + UUID.randomUUID().toString().substring(0, 8);

        // 统计告警级别
        long criticalCount = alerts.stream()
            .filter(a -> "critical".equalsIgnoreCase(a.getSeverity()))
            .count();
        long warningCount = alerts.stream()
            .filter(a -> "warning".equalsIgnoreCase(a.getSeverity()))
            .count();
        long infoCount = alerts.stream()
            .filter(a -> "info".equalsIgnoreCase(a.getSeverity()))
            .count();

        String summary = String.format(
            "总计 %d 条告警: 严重(%d) 警告(%d) 信息(%d)",
            alerts.size(), criticalCount, warningCount, infoCount
        );

        AlertBatch batch = AlertBatch.builder()
            .batchNo(batchNo)
            .windowStart(LocalDateTime.now().minusSeconds(windowSize))
            .windowEnd(LocalDateTime.now())
            .status("open")
            .alertCount(alerts.size())
            .summary(summary)
            .build();

        return batchRepository.save(batch);
    }

    /**
     * 更新告警状态（使用批量更新避免 N+1 查询）
     */
    @Transactional
    public void updateAlertsStatus(List<Long> alertIds, Long batchId) {
        if (alertIds == null || alertIds.isEmpty()) {
            return;
        }
        alertRepository.batchUpdateStatusAndBatchId(alertIds, "aggregated", batchId);
    }

    /**
     * 手动触发聚合
     */
    @Transactional
    public AlertBatch manualAggregate(List<Long> alertIds) {
        // 限制批量操作大小，防止资源耗尽
        if (alertIds == null || alertIds.isEmpty()) {
            throw new IllegalArgumentException("告警ID列表不能为空");
        }
        if (alertIds.size() > maxBatchSize) {
            throw new IllegalArgumentException("批量操作数量超过限制: " + maxBatchSize);
        }

        List<Alert> alerts = alertRepository.findAllById(alertIds);

        if (alerts.isEmpty()) {
            throw new RuntimeException("没有找到指定的告警");
        }

        AlertBatch batch = createBatch(alerts);
        updateAlertsStatus(alertIds, batch.getId());

        notificationService.sendBatchNotification(batch, alerts);

        batch.setStatus("sent");
        batch.setSentAt(LocalDateTime.now());

        return batchRepository.save(batch);
    }

    /**
     * 获取批次列表
     */
    public List<AlertBatch> getRecentBatches() {
        return batchRepository.findTop10ByOrderByCreatedAtDesc();
    }

    /**
     * 统计批次数量
     */
    public long countBatches() {
        return batchRepository.count();
    }

    /**
     * 统计已发送批次数量
     */
    public long countSentBatches() {
        return batchRepository.countByStatus("sent");
    }
}

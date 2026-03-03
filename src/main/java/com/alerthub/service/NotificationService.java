package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.model.AlertBatch;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.BatchStatus;
import com.alerthub.repository.AlertBatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 飞书通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AlertBatchRepository batchRepository;
    private final WebClient webClient;
    private final AlertHubProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 定时发送通知
     * 每 30 秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void sendPendingNotifications() {
        if (!properties.getNotification().getFeishu().isEnabled()) {
            return;
        }

        List<AlertBatch> pendingBatches = batchRepository.findPendingNotificationBatches();
        log.debug("Found {} batches pending notification", pendingBatches.size());

        for (AlertBatch batch : pendingBatches) {
            // 检查严重级别是否满足通知条件
            if (!shouldNotify(batch.getSeverity())) {
                log.debug("Batch {} severity {} below notification threshold, skipping",
                        batch.getId(), batch.getSeverity());
                markBatchCompleted(batch);
                continue;
            }

            try {
                sendFeishuNotification(batch);
            } catch (Exception e) {
                log.error("Failed to send notification for batch {}: {}", batch.getId(), e.getMessage(), e);
                handleNotificationFailure(batch, e);
            }
        }
    }

    /**
     * 发送飞书通知
     */
    private void sendFeishuNotification(AlertBatch batch) {
        log.info("Sending Feishu notification for batch {}", batch.getId());

        // 更新状态
        batch.setStatus(BatchStatus.NOTIFYING);
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);

        // 构建飞书卡片消息
        Map<String, Object> message = buildFeishuCardMessage(batch);

        String webhookUrl = properties.getNotification().getFeishu().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Feishu webhook URL is not configured, skipping notification");
            markBatchCompleted(batch);
            return;
        }

        try {
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            // 更新通知时间
            batch.setNotifiedAt(LocalDateTime.now());
            markBatchCompleted(batch);

            log.info("Feishu notification sent successfully for batch {}", batch.getId());
        } catch (WebClientResponseException e) {
            log.error("Feishu returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Feishu API error: " + e.getMessage());
        }
    }

    /**
     * 构建飞书卡片消息
     */
    private Map<String, Object> buildFeishuCardMessage(AlertBatch batch) {
        Map<String, Object> message = new HashMap<>();
        message.put("msg_type", "interactive");

        Map<String, Object> card = new HashMap<>();

        // 卡片配置
        Map<String, Object> config = new HashMap<>();
        config.put("wide_screen_mode", true);
        card.put("config", config);

        // 卡片头部
        Map<String, Object> header = new HashMap<>();
        header.put("title", createTextElement(formatTitle(batch)));
        header.put("template", getSeverityColor(batch.getSeverity()));
        card.put("header", header);

        // 卡片内容
        List<Map<String, Object>> elements = List.of(
                createMarkdownElement(formatBatchContent(batch))
        );
        card.put("elements", elements);

        message.put("card", card);
        return message;
    }

    /**
     * 格式化标题
     */
    private String formatTitle(AlertBatch batch) {
        return String.format("🚨 告警聚合通知 [%s]", batch.getSeverity().name());
    }

    /**
     * 格式化批次内容
     */
    private String formatBatchContent(AlertBatch batch) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("**告警来源**: %s\n\n", batch.getSource()));
        content.append(String.format("**严重级别**: %s\n\n", batch.getSeverity().name()));
        content.append(String.format("**告警数量**: %d\n\n", batch.getAlertCount()));
        content.append(String.format("**时间窗口**: %s ~ %s\n\n",
                batch.getWindowStart().toString(),
                batch.getWindowEnd().toString()));

        if (batch.getAnalysisResult() != null && !batch.getAnalysisResult().isEmpty()) {
            content.append(String.format("**分析结果**:\n%s\n\n", batch.getAnalysisResult()));
        }

        return content.toString();
    }

    /**
     * 根据严重级别获取颜色
     */
    private String getSeverityColor(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "red";
            case HIGH -> "orange";
            case MEDIUM -> "yellow";
            case LOW -> "blue";
            case INFO -> "grey";
        };
    }

    /**
     * 创建文本元素
     */
    private Map<String, Object> createTextElement(String content) {
        Map<String, Object> element = new HashMap<>();
        element.put("tag", "plain_text");
        element.put("content", content);
        return element;
    }

    /**
     * 创建 Markdown 元素
     */
    private Map<String, Object> createMarkdownElement(String content) {
        Map<String, Object> element = new HashMap<>();
        element.put("tag", "markdown");
        element.put("content", content);
        return element;
    }

    /**
     * 检查是否应该发送通知
     */
    private boolean shouldNotify(AlertSeverity severity) {
        String minSeverity = properties.getNotification().getFeishu().getMinSeverity();
        AlertSeverity threshold = AlertSeverity.fromString(minSeverity);
        return severity.isAtLeast(threshold);
    }

    /**
     * 标记批次完成
     */
    private void markBatchCompleted(AlertBatch batch) {
        batch.setStatus(BatchStatus.COMPLETED);
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);
    }

    /**
     * 处理通知失败
     */
    private void handleNotificationFailure(AlertBatch batch, Exception e) {
        batch.setStatus(BatchStatus.FAILED);
        batch.setErrorMessage("Notification failed: " + e.getMessage());
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);
    }

    /**
     * 手动发送通知
     */
    @Transactional
    public void sendNotification(Long batchId) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            if (shouldNotify(batch.getSeverity())) {
                sendFeishuNotification(batch);
            } else {
                log.info("Batch {} severity below threshold, skipping notification", batchId);
            }
        });
    }

    /**
     * 重试失败的通知
     */
    @Transactional
    public void retryNotification(Long batchId) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            if (batch.getStatus() == BatchStatus.FAILED) {
                batch.setStatus(BatchStatus.PENDING_NOTIFICATION);
                batch.setUpdatedAt(LocalDateTime.now());
                batch.setErrorMessage(null);
                batchRepository.save(batch);
                log.info("Batch {} queued for notification retry", batchId);
            }
        });
    }
}

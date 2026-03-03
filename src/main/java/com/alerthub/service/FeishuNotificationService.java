package com.alerthub.service;

import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 飞书通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuNotificationService {

    @Value("${alerthub.feishu.enabled:false}")
    private boolean enabled;

    @Value("${alerthub.feishu.webhook-url:}")
    private String webhookUrl;

    @Value("${alerthub.feishu.secret:}")
    private String secret;

    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送批次通知（异步方式，避免阻塞响应式线程）
     */
    public void sendBatchNotification(AlertBatch batch, List<Alert> alerts) {
        if (!enabled) {
            log.info("飞书通知未启用");
            return;
        }

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("飞书 Webhook URL 未配置");
            return;
        }

        try {
            Map<String, Object> message = buildBatchCardMessage(batch, alerts);
            String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
            String sign = generateSignature(timestamp);

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", timestamp);
            body.put("sign", sign);
            body.put("msg_type", "interactive");
            body.put("card", message);

            // 使用异步方式发送，避免阻塞响应式线程
            webClient.post()
                .uri(webhookUrl)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(
                    response -> {
                        log.info("飞书通知发送成功: batchNo={}, response={}", batch.getBatchNo(), response);
                        batch.setNotificationResult(response);
                    },
                    error -> {
                        log.error("飞书通知发送失败: batchNo={}", batch.getBatchNo(), error);
                        batch.setNotificationResult("发送失败: " + error.getMessage());
                    }
                );

            log.info("飞书通知已提交发送: batchNo={}", batch.getBatchNo());

        } catch (Exception e) {
            log.error("飞书通知发送失败: batchNo={}", batch.getBatchNo(), e);
            batch.setNotificationResult("发送失败: " + e.getMessage());
        }
    }

    /**
     * 构建批次卡片消息
     */
    private Map<String, Object> buildBatchCardMessage(AlertBatch batch, List<Alert> alerts) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("config", Map.of("wide_screen_mode", true));

        // 标题
        card.put("header", Map.of(
            "title", Map.of(
                "tag", "plain_text",
                "content", "🚨 告警聚合通知"
            ),
            "template", getHeaderColor(alerts)
        ));

        // 元素列表
        List<Map<String, Object>> elements = new ArrayList<>();

        // 批次信息
        elements.add(Map.of(
            "tag", "div",
            "fields", List.of(
                Map.of(
                    "is_short", true,
                    "text", Map.of(
                        "tag", "lark_md",
                        "content", "**批次号:**\n" + batch.getBatchNo()
                    )
                ),
                Map.of(
                    "is_short", true,
                    "text", Map.of(
                        "tag", "lark_md",
                        "content", "**时间窗口:**\n" +
                            batch.getWindowStart().format(TIME_FORMATTER) + " ~\n" +
                            batch.getWindowEnd().format(TIME_FORMATTER)
                    )
                )
            )
        ));

        // 统计信息
        elements.add(Map.of("tag", "hr"));

        long criticalCount = alerts.stream()
            .filter(a -> "critical".equalsIgnoreCase(a.getSeverity()))
            .count();
        long warningCount = alerts.stream()
            .filter(a -> "warning".equalsIgnoreCase(a.getSeverity()))
            .count();
        long infoCount = alerts.stream()
            .filter(a -> "info".equalsIgnoreCase(a.getSeverity()))
            .count();

        elements.add(Map.of(
            "tag", "div",
            "text", Map.of(
                "tag", "lark_md",
                "content", String.format(
                    "**告警统计:** 总计 %d 条 | 🔴 严重 %d | 🟡 警告 %d | 🔵 信息 %d",
                    alerts.size(), criticalCount, warningCount, infoCount
                )
            )
        ));

        // 告警列表（最多显示 5 条）
        elements.add(Map.of("tag", "hr"));

        int displayCount = Math.min(5, alerts.size());
        for (int i = 0; i < displayCount; i++) {
            Alert alert = alerts.get(i);
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of(
                    "tag", "lark_md",
                    "content", String.format(
                        "%s **%s**\n来源: %s | 时间: %s",
                        getSeverityEmoji(alert.getSeverity()),
                        alert.getTitle(),
                        alert.getSource(),
                        alert.getCreatedAt().format(TIME_FORMATTER)
                    )
                )
            ));
        }

        if (alerts.size() > 5) {
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of(
                    "tag", "lark_md",
                    "content", String.format("... 还有 %d 条告警未显示", alerts.size() - 5)
                )
            ));
        }

        card.put("elements", elements);

        return card;
    }

    /**
     * 获取标题颜色
     */
    private String getHeaderColor(List<Alert> alerts) {
        boolean hasCritical = alerts.stream()
            .anyMatch(a -> "critical".equalsIgnoreCase(a.getSeverity()));

        if (hasCritical) {
            return "red";
        }

        boolean hasWarning = alerts.stream()
            .anyMatch(a -> "warning".equalsIgnoreCase(a.getSeverity()));

        if (hasWarning) {
            return "orange";
        }

        return "blue";
    }

    /**
     * 获取级别对应的 emoji
     */
    private String getSeverityEmoji(String severity) {
        if (severity == null) return "⚪";
        return switch (severity.toLowerCase()) {
            case "critical" -> "🔴";
            case "warning" -> "🟡";
            case "info" -> "🔵";
            default -> "⚪";
        };
    }

    /**
     * 生成签名
     */
    private String generateSignature(String timestamp) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }

        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[]{});
            return Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成签名失败", e);
            return "";
        }
    }
}

package com.alerthub.service;

import com.alerthub.a2a.A2AResponse;
import com.alerthub.config.AlertHubProperties;
import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

/**
 * 飞书通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuService {

    private final WebClient.Builder webClientBuilder;
    private final AlertHubProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 发送告警通知
     */
    public void sendAlertNotification(Alert alert) {
        if (!isFeishuEnabled()) {
            log.debug("飞书通知未启用");
            return;
        }

        try {
            Map<String, Object> card = buildAlertCard(alert);
            sendFeishuMessage(card);
            log.info("飞书告警通知发送成功: alertId={}", alert.getId());
        } catch (Exception e) {
            log.error("飞书告警通知发送失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送批次通知
     */
    public void sendBatchNotification(AlertBatch batch, List<Alert> alerts, A2AResponse analysisResult) {
        if (!isFeishuEnabled()) {
            log.debug("飞书通知未启用");
            return;
        }

        try {
            Map<String, Object> card = buildBatchCard(batch, alerts, analysisResult);
            String messageId = sendFeishuMessage(card);

            // 更新批次的消息ID
            batch.setFeishuMessageId(messageId);
            log.info("飞书批次通知发送成功: batchNo={}, alertCount={}", batch.getBatchNo(), alerts.size());
        } catch (Exception e) {
            log.error("飞书批次通知发送失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送飞书消息
     */
    private String sendFeishuMessage(Map<String, Object> content) {
        String webhookUrl = properties.getFeishu().getWebhookUrl();
        String secret = properties.getFeishu().getSecret();

        long timestamp = System.currentTimeMillis() / 1000;
        String sign = null;

        if (secret != null && !secret.isEmpty()) {
            sign = generateSign(timestamp, secret);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("msg_type", "interactive");
        body.put("card", content);

        if (sign != null) {
            body.put("timestamp", timestamp);
            body.put("sign", sign);
        }

        WebClient webClient = webClientBuilder.build();

        Map<String, Object> response = webClient.post()
            .uri(webhookUrl)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .timeout(Duration.ofSeconds(10))
            .block();

        log.debug("飞书响应: {}", response);
        return response != null ? String.valueOf(response.get("StatusCode")) : null;
    }

    /**
     * 构建告警卡片
     */
    private Map<String, Object> buildAlertCard(Alert alert) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("header", Map.of(
            "title", Map.of("content", "🚨 " + alert.getAlertName(), "tag", "plain_text"),
            "template", getSeverityColor(alert.getSeverity())
        ));

        List<Map<String, Object>> elements = new ArrayList<>();

        // 告警信息
        elements.add(Map.of(
            "tag", "div",
            "fields", List.of(
                Map.of("is_short", true, "text", Map.of("content", "**来源:** " + alert.getSource(), "tag", "lark_md")),
                Map.of("is_short", true, "text", Map.of("content", "**严重程度:** " + alert.getSeverity(), "tag", "lark_md"))
            )
        ));

        if (alert.getMessage() != null && !alert.getMessage().isEmpty()) {
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of("content", "**消息:**\n" + alert.getMessage(), "tag", "lark_md")
            ));
        }

        // 时间信息
        elements.add(Map.of(
            "tag", "div",
            "fields", List.of(
                Map.of("is_short", true, "text", Map.of("content", "**创建时间:** " + alert.getCreatedAt(), "tag", "lark_md")),
                Map.of("is_short", true, "text", Map.of("content", "**指纹:** " + alert.getFingerprint().substring(0, 8) + "...", "tag", "lark_md"))
            )
        ));

        card.put("elements", elements);
        return card;
    }

    /**
     * 构建批次卡片
     */
    private Map<String, Object> buildBatchCard(AlertBatch batch, List<Alert> alerts, A2AResponse analysisResult) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("header", Map.of(
            "title", Map.of("content", "📦 " + batch.getBatchNo(), "tag", "plain_text"),
            "template", "blue"
        ));

        List<Map<String, Object>> elements = new ArrayList<>();

        // 批次概要
        elements.add(Map.of(
            "tag", "div",
            "fields", List.of(
                Map.of("is_short", true, "text", Map.of("content", "**告警数量:** " + alerts.size(), "tag", "lark_md")),
                Map.of("is_short", true, "text", Map.of("content", "**状态:** " + batch.getStatus(), "tag", "lark_md"))
            )
        ));

        // 严重程度统计
        Map<String, Long> severityCount = new LinkedHashMap<>();
        for (Alert alert : alerts) {
            severityCount.merge(alert.getSeverity(), 1L, Long::sum);
        }
        String severityStats = severityCount.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .reduce((a, b) -> a + " | " + b)
            .orElse("");
        elements.add(Map.of(
            "tag", "div",
            "text", Map.of("content", "**严重程度分布:** " + severityStats, "tag", "lark_md")
        ));

        // 根因分析结果
        if (analysisResult != null && analysisResult.isSuccess() && analysisResult.getResult() != null) {
            A2AResponse.AnalysisResult result = analysisResult.getResult();
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of("content", "---", "tag", "lark_md")
            ));
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of("content", "**🔍 根因分析结果**", "tag", "lark_md")
            ));
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of("content", "**类型:** " + result.getRootCauseType(), "tag", "lark_md")
            ));
            elements.add(Map.of(
                "tag", "div",
                "text", Map.of("content", "**描述:** " + result.getRootCauseDescription(), "tag", "lark_md")
            ));
            if (result.getConfidence() != null) {
                elements.add(Map.of(
                    "tag", "div",
                    "text", Map.of("content", "**置信度:** " + result.getConfidence() + "%", "tag", "lark_md")
                ));
            }
            if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
                String recommendations = String.join("\n- ", result.getRecommendations());
                elements.add(Map.of(
                    "tag", "div",
                    "text", Map.of("content", "**建议操作:**\n- " + recommendations, "tag", "lark_md")
                ));
            }
        }

        card.put("elements", elements);
        return card;
    }

    /**
     * 获取严重程度对应的颜色
     */
    private String getSeverityColor(String severity) {
        return switch (severity.toLowerCase()) {
            case "critical" -> "red";
            case "high" -> "orange";
            case "medium" -> "yellow";
            case "low" -> "blue";
            default -> "grey";
        };
    }

    /**
     * 生成签名
     */
    private String generateSign(long timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[0]);
            return Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("生成签名失败", e);
            return null;
        }
    }

    /**
     * 检查飞书是否启用
     */
    private boolean isFeishuEnabled() {
        return properties.getFeishu().isEnabled() &&
            properties.getFeishu().getWebhookUrl() != null &&
            !properties.getFeishu().getWebhookUrl().isEmpty();
    }
}

package com.alerthub.service;

import com.alerthub.a2a.A2ARequest;
import com.alerthub.a2a.A2AResponse;
import com.alerthub.config.AlertHubProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * A2A 协议服务
 * 用于调用 Sub-Agent 进行根因分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class A2AService {

    private final WebClient.Builder webClientBuilder;
    private final AlertHubProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 调用 Sub-Agent 进行根因分析
     */
    public A2AResponse analyzeRootCause(com.alerthub.entity.Alert alert, String batchNo) {
        if (!properties.getA2a().isEnabled()) {
            log.info("A2A 服务未启用，跳过根因分析");
            return createMockResponse(alert);
        }

        A2ARequest request = A2ARequest.forRootCauseAnalysis(alert, batchNo);
        return callAgent(request);
    }

    /**
     * 批量根因分析
     */
    public A2AResponse analyzeBatchRootCause(List<com.alerthub.entity.Alert> alerts, String batchNo) {
        if (!properties.getA2a().isEnabled()) {
            log.info("A2A 服务未启用，跳过批量根因分析");
            return createMockBatchResponse(alerts);
        }

        A2ARequest request = A2ARequest.forBatchRootCauseAnalysis(alerts, batchNo);
        return callAgent(request);
    }

    /**
     * 调用 Agent
     */
    private A2AResponse callAgent(A2ARequest request) {
        String agentUrl = properties.getA2a().getAgentUrl();
        int timeoutSeconds = properties.getA2a().getTimeoutSeconds();
        int retryCount = properties.getA2a().getRetryCount();

        try {
            WebClient webClient = webClientBuilder
                .baseUrl(agentUrl)
                .build();

            A2AResponse response = webClient.post()
                .uri("/analyze")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(A2AResponse.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.fixedDelay(retryCount, Duration.ofSeconds(1))
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                        log.warn("A2A 调用重试 {} 次后仍失败", retrySignal.totalRetries());
                        return retrySignal.failure();
                    }))
                .onErrorResume(e -> {
                    log.error("A2A 调用失败: {}", e.getMessage());
                    return Mono.just(createErrorResponse(request.getTaskId(), e.getMessage()));
                })
                .block();

            log.info("A2A 调用完成: taskId={}, status={}", request.getTaskId(),
                response != null ? response.getStatus() : "null");
            return response;

        } catch (Exception e) {
            log.error("A2A 调用异常: {}", e.getMessage(), e);
            return createErrorResponse(request.getTaskId(), e.getMessage());
        }
    }

    /**
     * 创建模拟响应（A2A 服务未启用时使用）
     */
    private A2AResponse createMockResponse(com.alerthub.entity.Alert alert) {
        return A2AResponse.builder()
            .taskId(java.util.UUID.randomUUID().toString())
            .status("mock")
            .result(A2AResponse.AnalysisResult.builder()
                .rootCauseType("unknown")
                .rootCauseDescription("A2A 服务未启用，无法进行根因分析")
                .confidence(0)
                .recommendations(List.of("启用 A2A 服务以获取根因分析"))
                .build())
            .processedAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 创建批量模拟响应
     */
    private A2AResponse createMockBatchResponse(List<com.alerthub.entity.Alert> alerts) {
        return A2AResponse.builder()
            .taskId(java.util.UUID.randomUUID().toString())
            .status("mock")
            .result(A2AResponse.AnalysisResult.builder()
                .rootCauseType("batch_analysis")
                .rootCauseDescription(String.format("批量分析 %d 个告警（A2A 服务未启用）", alerts.size()))
                .confidence(0)
                .recommendations(List.of("启用 A2A 服务以获取批量根因分析"))
                .build())
            .processedAt(java.time.LocalDateTime.now())
            .build();
    }

    /**
     * 创建错误响应
     */
    private A2AResponse createErrorResponse(String taskId, String errorMessage) {
        return A2AResponse.builder()
            .taskId(taskId)
            .status("error")
            .error(errorMessage)
            .processedAt(java.time.LocalDateTime.now())
            .build();
    }
}

package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.model.AlertBatch;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.BatchStatus;
import com.alerthub.repository.AlertBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * A2A 分析服务
 * 调用外部 AI 分析服务进行智能分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AlertBatchRepository batchRepository;
    private final WebClient webClient;
    private final AlertHubProperties properties;

    /**
     * 定时处理待分析的批次
     * 每 30 秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void processPendingAnalysis() {
        if (!properties.getAnalysis().isEnabled()) {
            return;
        }

        List<AlertBatch> pendingBatches = batchRepository.findPendingAnalysisBatches();
        log.debug("Found {} batches pending analysis", pendingBatches.size());

        for (AlertBatch batch : pendingBatches) {
            try {
                analyzeBatch(batch);
            } catch (Exception e) {
                log.error("Failed to analyze batch {}: {}", batch.getId(), e.getMessage(), e);
                handleAnalysisFailure(batch, e);
            }
        }
    }

    /**
     * 分析单个批次
     */
    private void analyzeBatch(AlertBatch batch) {
        log.info("Analyzing batch {}", batch.getId());

        // 更新状态为分析中
        batch.setStatus(BatchStatus.ANALYZING);
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);

        // 构建分析请求
        Map<String, Object> analysisRequest = buildAnalysisRequest(batch);

        // 调用外部分析服务
        String analysisResult = callAnalysisService(analysisRequest);

        // 更新分析结果
        batch.setAnalysisResult(analysisResult);
        batch.setAnalyzedAt(LocalDateTime.now());
        batch.setStatus(BatchStatus.PENDING_NOTIFICATION);
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);

        log.info("Batch {} analysis completed", batch.getId());
    }

    /**
     * 构建分析请求
     */
    private Map<String, Object> buildAnalysisRequest(AlertBatch batch) {
        Map<String, Object> request = new HashMap<>();
        request.put("batchId", batch.getId());
        request.put("source", batch.getSource());
        request.put("severity", batch.getSeverity().name());
        request.put("alertCount", batch.getAlertCount());
        request.put("windowStart", batch.getWindowStart().toString());
        request.put("windowEnd", batch.getWindowEnd().toString());
        request.put("summary", batch.getSummary());
        return request;
    }

    /**
     * 调用外部分析服务
     */
    private String callAnalysisService(Map<String, Object> request) {
        String endpoint = properties.getAnalysis().getEndpoint();
        int timeoutSeconds = properties.getAnalysis().getTimeoutSeconds();

        try {
            return webClient.post()
                    .uri(endpoint)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Analysis service returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Analysis service error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to call analysis service: {}", e.getMessage());
            throw new RuntimeException("Failed to call analysis service: " + e.getMessage());
        }
    }

    /**
     * 处理分析失败
     */
    private void handleAnalysisFailure(AlertBatch batch, Exception e) {
        batch.setStatus(BatchStatus.FAILED);
        batch.setErrorMessage("Analysis failed: " + e.getMessage());
        batch.setUpdatedAt(LocalDateTime.now());
        batchRepository.save(batch);
    }

    /**
     * 手动触发分析
     */
    @Transactional
    public void triggerAnalysis(Long batchId) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            if (batch.getStatus() == BatchStatus.PENDING_ANALYSIS) {
                try {
                    analyzeBatch(batch);
                } catch (Exception e) {
                    handleAnalysisFailure(batch, e);
                    throw e;
                }
            } else {
                log.warn("Batch {} is not in PENDING_ANALYSIS status, current status: {}",
                        batchId, batch.getStatus());
            }
        });
    }

    /**
     * 跳过分析（直接进入通知阶段）
     */
    @Transactional
    public void skipAnalysis(Long batchId) {
        batchRepository.findById(batchId).ifPresent(batch -> {
            batch.setStatus(BatchStatus.PENDING_NOTIFICATION);
            batch.setUpdatedAt(LocalDateTime.now());
            batch.setAnalysisResult("Analysis skipped");
            batchRepository.save(batch);
            log.info("Skipped analysis for batch {}", batchId);
        });
    }
}

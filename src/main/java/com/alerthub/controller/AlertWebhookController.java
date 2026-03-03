package com.alerthub.controller;

import com.alerthub.dto.AlertRequest;
import com.alerthub.dto.AlertResponse;
import com.alerthub.dto.ApiResponse;
import com.alerthub.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 告警 Webhook 接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Webhook", description = "告警接收接口")
public class AlertWebhookController {

    private final AlertService alertService;

    /**
     * 接收告警
     */
    @PostMapping
    @Operation(summary = "接收告警", description = "通过 Webhook 接收外部告警数据")
    public ResponseEntity<ApiResponse<AlertResponse>> receiveAlert(
            @Valid @RequestBody AlertRequest request) {
        log.info("Received alert from source: {}", request.getSource());

        AlertResponse response = alertService.receiveAlert(request);

        if (response.isDuplicate()) {
            return ResponseEntity.ok(ApiResponse.success("告警已接收（重复告警）", response));
        } else {
            return ResponseEntity.ok(ApiResponse.success("告警接收成功", response));
        }
    }

    /**
     * 批量接收告警
     */
    @PostMapping("/batch")
    @Operation(summary = "批量接收告警", description = "批量接收多个告警")
    public ResponseEntity<ApiResponse<java.util.List<AlertResponse>>> receiveAlerts(
            @Valid @RequestBody java.util.List<@Valid AlertRequest> requests) {
        log.info("Received batch of {} alerts", requests.size());

        java.util.List<AlertResponse> responses = requests.stream()
                .map(alertService::receiveAlert)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(
                String.format("成功接收 %d 条告警", responses.size()),
                responses));
    }

    /**
     * Prometheus Alertmanager Webhook 格式
     */
    @PostMapping("/alertmanager")
    @Operation(summary = "Alertmanager Webhook", description = "接收 Prometheus Alertmanager 的告警")
    public ResponseEntity<ApiResponse<Void>> receiveAlertmanagerAlert(
            @RequestBody String rawPayload) {
        log.info("Received Alertmanager webhook");

        // 解析 Alertmanager 格式并转换为标准格式
        // 这里简化处理，实际需要解析 Alertmanager 的 JSON 格式
        AlertRequest request = AlertRequest.builder()
                .source("prometheus")
                .severity("HIGH")
                .title("Alertmanager Alert")
                .description("Alert from Prometheus Alertmanager")
                .rawPayload(rawPayload)
                .build();

        alertService.receiveAlert(request);

        return ResponseEntity.ok(ApiResponse.success("Alertmanager 告警接收成功", null));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查服务是否正常运行")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("healthy", "Alert Hub is running"));
    }
}

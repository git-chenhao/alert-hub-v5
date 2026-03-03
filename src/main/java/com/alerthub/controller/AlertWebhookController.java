package com.alerthub.controller;

import com.alerthub.dto.AlertRequest;
import com.alerthub.dto.AlertResponse;
import com.alerthub.entity.Alert;
import com.alerthub.service.AlertService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警 Webhook 接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertWebhookController {

    private final AlertService alertService;

    /**
     * 接收告警
     */
    @PostMapping
    public ResponseEntity<AlertResponse<Alert>> receiveAlert(
        @Valid @RequestBody AlertRequest request) {
        log.info("收到告警请求: {}", request.getTitle());

        Alert alert = alertService.receiveAlert(request);

        if (alert == null) {
            return ResponseEntity.ok(
                AlertResponse.success("告警重复，已去重", null)
            );
        }

        return ResponseEntity.ok(
            AlertResponse.success("告警已接收", alert)
        );
    }

    /**
     * 批量接收告警
     */
    @PostMapping("/batch")
    public ResponseEntity<AlertResponse<List<Alert>>> receiveAlerts(
        @Size(min = 1, max = 100, message = "批量请求数量必须在 1-100 之间")
        @Valid @RequestBody List<AlertRequest> requests) {
        log.info("收到批量告警请求: {} 条", requests.size());

        List<Alert> alerts = requests.stream()
            .map(alertService::receiveAlert)
            .filter(a -> a != null)
            .toList();

        return ResponseEntity.ok(
            AlertResponse.success(
                String.format("成功接收 %d 条告警", alerts.size()),
                alerts
            )
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<AlertResponse<String>> health() {
        return ResponseEntity.ok(
            AlertResponse.success("Alert Hub V5 is running", "OK")
        );
    }
}

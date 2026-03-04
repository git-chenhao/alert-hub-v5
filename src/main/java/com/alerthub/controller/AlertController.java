package com.alerthub.controller;

import com.alerthub.a2a.A2AResponse;
import com.alerthub.dto.AlertReceivedData;
import com.alerthub.dto.AlertRequest;
import com.alerthub.dto.AlertResponse;
import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.alerthub.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 告警 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * 接收告警 Webhook
     * POST /api/alerts
     */
    @PostMapping("/alerts")
    public ResponseEntity<AlertResponse<AlertReceivedData>> receiveAlert(
            @Valid @RequestBody AlertRequest request) {
        log.info("收到告警: source={}, name={}, severity={}",
            request.getSource(), request.getAlertName(), request.getSeverity());

        AlertReceivedData data = alertService.receiveAlert(request);

        if (data.isDuplicate()) {
            return ResponseEntity.ok(AlertResponse.success("告警已去重", data));
        }
        return ResponseEntity.ok(AlertResponse.success("告警接收成功", data));
    }

    /**
     * 获取告警列表
     * GET /api/alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<AlertResponse<Page<Alert>>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Alert> alerts = alertService.getAlerts(status, pageRequest);

        return ResponseEntity.ok(AlertResponse.success(alerts));
    }

    /**
     * 获取告警详情
     * GET /api/alerts/{id}
     */
    @GetMapping("/alerts/{id}")
    public ResponseEntity<AlertResponse<Alert>> getAlert(@PathVariable Long id) {
        Optional<Alert> alert = alertService.getAlert(id);

        return alert.map(a -> ResponseEntity.ok(AlertResponse.success(a)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新告警状态
     * PUT /api/alerts/{id}/status
     */
    @PutMapping("/alerts/{id}/status")
    public ResponseEntity<AlertResponse<Alert>> updateAlertStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String status = body.get("status");
        if (status == null || status.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(AlertResponse.error(400, "状态不能为空"));
        }

        Alert alert = alertService.updateAlertStatus(id, status);
        return ResponseEntity.ok(AlertResponse.success("状态更新成功", alert));
    }

    /**
     * 触发根因分析
     * POST /api/alerts/{id}/analyze
     */
    @PostMapping("/alerts/{id}/analyze")
    public ResponseEntity<AlertResponse<A2AResponse>> triggerAnalysis(@PathVariable Long id) {
        A2AResponse response = alertService.triggerRootCauseAnalysis(id);
        return ResponseEntity.ok(AlertResponse.success(response));
    }

    /**
     * 获取批次列表
     * GET /api/batches
     */
    @GetMapping("/batches")
    public ResponseEntity<AlertResponse<Page<AlertBatch>>> getBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertBatch> batches = alertService.getBatches(pageRequest);

        return ResponseEntity.ok(AlertResponse.success(batches));
    }

    /**
     * 获取批次详情
     * GET /api/batches/{id}
     */
    @GetMapping("/batches/{id}")
    public ResponseEntity<AlertResponse<AlertBatch>> getBatch(@PathVariable Long id) {
        Optional<AlertBatch> batch = alertService.getBatch(id);

        return batch.map(b -> ResponseEntity.ok(AlertResponse.success(b)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取批次内告警
     * GET /api/batches/{id}/alerts
     */
    @GetMapping("/batches/{id}/alerts")
    public ResponseEntity<AlertResponse<List<Alert>>> getBatchAlerts(@PathVariable Long id) {
        List<Alert> alerts = alertService.getBatchAlerts(id);
        return ResponseEntity.ok(AlertResponse.success(alerts));
    }

    /**
     * 获取统计信息
     * GET /api/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<AlertResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = alertService.getStatistics();
        return ResponseEntity.ok(AlertResponse.success(stats));
    }

    /**
     * 健康检查
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<AlertResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(AlertResponse.success(Map.of(
            "status", "UP",
            "service", "alert-hub-v5",
            "timestamp", java.time.LocalDateTime.now().toString()
        )));
    }
}

package com.alerthub.controller;

import com.alerthub.dto.ApiResponse;
import com.alerthub.dto.BatchResponse;
import com.alerthub.exception.ResourceNotFoundException;
import com.alerthub.model.Alert;
import com.alerthub.model.AlertBatch;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.AlertStatus;
import com.alerthub.repository.AlertBatchRepository;
import com.alerthub.repository.AlertRepository;
import com.alerthub.service.AggregationService;
import com.alerthub.service.AnalysisService;
import com.alerthub.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理接口控制器
 */
@Slf4j
@Controller
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "管理接口")
public class AdminController {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository batchRepository;
    private final AggregationService aggregationService;
    private final AnalysisService analysisService;
    private final NotificationService notificationService;

    // ==================== REST API ====================

    /**
     * 获取告警列表
     */
    @GetMapping("/alerts")
    @ResponseBody
    @Operation(summary = "获取告警列表", description = "分页查询告警列表")
    public ResponseEntity<ApiResponse<Page<Alert>>> getAlerts(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "receivedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        AlertSeverity severityEnum = severity != null ? AlertSeverity.fromString(severity) : null;
        AlertStatus statusEnum = status != null ? AlertStatus.valueOf(status) : null;

        Page<Alert> alerts = alertRepository.searchAlerts(source, severityEnum, statusEnum, startTime, endTime, pageable);

        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * 获取告警详情
     */
    @GetMapping("/alerts/{id}")
    @ResponseBody
    @Operation(summary = "获取告警详情", description = "根据 ID 获取告警详情")
    public ResponseEntity<ApiResponse<Alert>> getAlert(@PathVariable Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.alert(id));
        return ResponseEntity.ok(ApiResponse.success(alert));
    }

    /**
     * 获取批次列表
     */
    @GetMapping("/batches")
    @ResponseBody
    @Operation(summary = "获取批次列表", description = "分页查询批次列表")
    public ResponseEntity<ApiResponse<Page<BatchResponse>>> getBatches(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        AlertSeverity severityEnum = severity != null ? AlertSeverity.fromString(severity) : null;
        com.alerthub.model.BatchStatus statusEnum = status != null
                ? com.alerthub.model.BatchStatus.valueOf(status) : null;

        Page<AlertBatch> batches = batchRepository.searchBatches(source, severityEnum, statusEnum, startTime, endTime, pageable);

        Page<BatchResponse> responses = batches.map(this::toBatchResponse);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 获取批次详情
     */
    @GetMapping("/batches/{id}")
    @ResponseBody
    @Operation(summary = "获取批次详情", description = "根据 ID 获取批次详情")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatch(@PathVariable Long id) {
        AlertBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.batch(id));
        return ResponseEntity.ok(ApiResponse.success(toBatchResponse(batch)));
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    @ResponseBody
    @Operation(summary = "获取统计信息", description = "获取告警和批次的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 告警统计
        stats.put("totalAlerts", alertRepository.count());
        stats.put("receivedAlerts", alertRepository.countByStatus(AlertStatus.RECEIVED));
        stats.put("duplicatedAlerts", alertRepository.countByStatus(AlertStatus.DUPLICATED));
        stats.put("aggregatedAlerts", alertRepository.countByStatus(AlertStatus.AGGREGATED));

        // 批次统计
        stats.put("totalBatches", batchRepository.count());
        stats.put("aggregatingBatches", batchRepository.countByStatus(com.alerthub.model.BatchStatus.AGGREGATING));
        stats.put("completedBatches", batchRepository.countByStatus(com.alerthub.model.BatchStatus.COMPLETED));
        stats.put("failedBatches", batchRepository.countByStatus(com.alerthub.model.BatchStatus.FAILED));

        // 最近 24 小时统计
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        stats.put("alertsLast24h", alertRepository.countSince(since));
        stats.put("batchesLast24h", batchRepository.countSince(since));

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 手动触发分析
     */
    @PostMapping("/batches/{id}/analyze")
    @ResponseBody
    @Operation(summary = "手动触发分析", description = "手动触发指定批次的分析")
    public ResponseEntity<ApiResponse<Void>> triggerAnalysis(@PathVariable Long id) {
        analysisService.triggerAnalysis(id);
        return ResponseEntity.ok(ApiResponse.success("分析已触发", null));
    }

    /**
     * 跳过分析
     */
    @PostMapping("/batches/{id}/skip-analysis")
    @ResponseBody
    @Operation(summary = "跳过分析", description = "跳过指定批次的分析")
    public ResponseEntity<ApiResponse<Void>> skipAnalysis(@PathVariable Long id) {
        analysisService.skipAnalysis(id);
        return ResponseEntity.ok(ApiResponse.success("已跳过分析", null));
    }

    /**
     * 手动发送通知
     */
    @PostMapping("/batches/{id}/notify")
    @ResponseBody
    @Operation(summary = "手动发送通知", description = "手动发送指定批次的通知")
    public ResponseEntity<ApiResponse<Void>> sendNotification(@PathVariable Long id) {
        notificationService.sendNotification(id);
        return ResponseEntity.ok(ApiResponse.success("通知已发送", null));
    }

    /**
     * 重试失败的通知
     */
    @PostMapping("/batches/{id}/retry")
    @ResponseBody
    @Operation(summary = "重试通知", description = "重试失败的批次通知")
    public ResponseEntity<ApiResponse<Void>> retryNotification(@PathVariable Long id) {
        notificationService.retryNotification(id);
        return ResponseEntity.ok(ApiResponse.success("已加入重试队列", null));
    }

    // ==================== Web UI ====================

    /**
     * 管理界面首页
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 获取统计信息
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAlerts", alertRepository.count());
        stats.put("totalBatches", batchRepository.count());
        stats.put("alertsLast24h", alertRepository.countSince(LocalDateTime.now().minusHours(24)));
        stats.put("batchesLast24h", batchRepository.countSince(LocalDateTime.now().minusHours(24)));

        model.addAttribute("stats", stats);

        // 获取最近的告警和批次
        model.addAttribute("recentAlerts", alertRepository.findAll(
                PageRequest.of(0, 10, Sort.by("receivedAt").descending())).getContent());
        model.addAttribute("recentBatches", batchRepository.findAll(
                PageRequest.of(0, 10, Sort.by("createdAt").descending())).getContent());

        return "admin/dashboard";
    }

    /**
     * 告警列表页面
     */
    @GetMapping("/alerts/view")
    public String alertsView(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Page<Alert> alerts = alertRepository.findAll(
                PageRequest.of(page, size, Sort.by("receivedAt").descending()));

        model.addAttribute("alerts", alerts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", alerts.getTotalPages());

        return "admin/alerts";
    }

    /**
     * 批次列表页面
     */
    @GetMapping("/batches/view")
    public String batchesView(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Page<AlertBatch> batches = batchRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("batches", batches);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", batches.getTotalPages());

        return "admin/batches";
    }

    /**
     * 转换 BatchResponse
     */
    private BatchResponse toBatchResponse(AlertBatch batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .batchKey(batch.getBatchKey())
                .source(batch.getSource())
                .severity(batch.getSeverity())
                .windowStart(batch.getWindowStart())
                .windowEnd(batch.getWindowEnd())
                .alertCount(batch.getAlertCount())
                .analysisResult(batch.getAnalysisResult())
                .summary(batch.getSummary())
                .status(batch.getStatus())
                .createdAt(batch.getCreatedAt())
                .analyzedAt(batch.getAnalyzedAt())
                .notifiedAt(batch.getNotifiedAt())
                .metadata(batch.getMetadata())
                .build();
    }
}

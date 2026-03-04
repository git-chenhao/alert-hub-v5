package com.alerthub.controller;

import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.alerthub.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 后台管理界面控制器
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final AlertService alertService;

    /**
     * 首页仪表盘
     */
    @GetMapping
    public String dashboard(Model model) {
        Map<String, Object> stats = alertService.getStatistics();
        model.addAttribute("stats", stats);

        // 最近告警
        Page<Alert> recentAlerts = alertService.getAlerts(null,
            PageRequest.of(0, 10, Sort.by("createdAt").descending()));
        model.addAttribute("recentAlerts", recentAlerts.getContent());

        // 最近批次
        Page<AlertBatch> recentBatches = alertService.getBatches(
            PageRequest.of(0, 5, Sort.by("createdAt").descending()));
        model.addAttribute("recentBatches", recentBatches.getContent());

        return "dashboard";
    }

    /**
     * 告警列表页
     */
    @GetMapping("/alerts")
    public String alerts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Page<Alert> alerts = alertService.getAlerts(status,
            PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("alerts", alerts);
        model.addAttribute("status", status);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", alerts.getTotalPages());

        return "alerts";
    }

    /**
     * 告警详情页
     */
    @GetMapping("/alerts/{id}")
    public String alertDetail(@PathVariable Long id, Model model) {
        Alert alert = alertService.getAlert(id)
            .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + id));

        model.addAttribute("alert", alert);

        // 如果有批次，获取批次信息
        if (alert.getBatchId() != null) {
            alertService.getBatch(alert.getBatchId())
                .ifPresent(batch -> model.addAttribute("batch", batch));
        }

        return "alert-detail";
    }

    /**
     * 批次列表页
     */
    @GetMapping("/batches")
    public String batches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Page<AlertBatch> batches = alertService.getBatches(
            PageRequest.of(page, size, Sort.by("createdAt").descending()));

        model.addAttribute("batches", batches);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", batches.getTotalPages());

        return "batches";
    }

    /**
     * 批次详情页
     */
    @GetMapping("/batches/{id}")
    public String batchDetail(@PathVariable Long id, Model model) {
        AlertBatch batch = alertService.getBatch(id)
            .orElseThrow(() -> new IllegalArgumentException("批次不存在: " + id));

        List<Alert> alerts = alertService.getBatchAlerts(id);

        model.addAttribute("batch", batch);
        model.addAttribute("alerts", alerts);

        return "batch-detail";
    }

    /**
     * 统计页面
     */
    @GetMapping("/statistics")
    public String statistics(Model model) {
        Map<String, Object> stats = alertService.getStatistics();
        model.addAttribute("stats", stats);
        return "statistics";
    }
}

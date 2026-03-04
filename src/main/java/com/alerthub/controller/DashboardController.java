package com.alerthub.controller;

import com.alerthub.entity.Alert;
import com.alerthub.entity.AlertBatch;
import com.alerthub.service.AggregationService;
import com.alerthub.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理界面 Controller
 */
@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final AlertService alertService;
    private final AggregationService aggregationService;

    /**
     * 首页
     */
    @GetMapping
    public String index(Model model) {
        // 统计数据
        model.addAttribute("totalAlerts", alertService.countAlerts());
        model.addAttribute("pendingAlerts", alertService.countPendingAlerts());
        model.addAttribute("totalBatches", aggregationService.countBatches());
        model.addAttribute("sentBatches", aggregationService.countSentBatches());

        // 最近告警
        model.addAttribute("recentAlerts", alertService.getRecentAlerts());

        // 最近批次
        model.addAttribute("recentBatches", aggregationService.getRecentBatches());

        return "admin/index";
    }

    /**
     * 告警列表
     */
    @GetMapping("/alerts")
    public String alerts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        Model model) {

        // 验证状态参数
        List<String> validStatuses = List.of("pending", "aggregated", "sent", "resolved");
        String validStatus = (status != null && validStatuses.contains(status)) ? status : null;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Alert> alertPage;
        if (validStatus != null) {
            alertPage = alertService.getAlertsByStatus(validStatus, pageRequest);
        } else {
            alertPage = alertService.getAllAlerts(pageRequest);
        }

        model.addAttribute("alertPage", alertPage);
        model.addAttribute("currentStatus", validStatus);
        model.addAttribute("statuses", validStatuses);

        return "admin/alerts";
    }

    /**
     * 告警详情
     */
    @GetMapping("/alerts/{id}")
    public String alertDetail(@PathVariable Long id, Model model) {
        Alert alert = alertService.findById(id)
            .orElseThrow(() -> new RuntimeException("告警不存在"));

        model.addAttribute("alert", alert);
        return "admin/alert-detail";
    }

    /**
     * 批次列表
     */
    @GetMapping("/batches")
    public String batches(Model model) {
        List<AlertBatch> batches = aggregationService.getRecentBatches();
        model.addAttribute("batches", batches);
        return "admin/batches";
    }

    /**
     * 手动聚合
     */
    @PostMapping("/aggregate")
    @ResponseBody
    public Map<String, Object> manualAggregate(@RequestBody List<Long> alertIds) {
        Map<String, Object> result = new HashMap<>();
        try {
            AlertBatch batch = aggregationService.manualAggregate(alertIds);
            result.put("success", true);
            result.put("batchNo", batch.getBatchNo());
            result.put("message", "聚合成功");
        } catch (Exception e) {
            log.error("手动聚合失败", e);
            result.put("success", false);
            result.put("message", "聚合失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 更新告警状态
     */
    @PostMapping("/alerts/{id}/status")
    @ResponseBody
    public Map<String, Object> updateAlertStatus(
        @PathVariable Long id,
        @RequestParam String status) {

        Map<String, Object> result = new HashMap<>();
        try {
            alertService.updateAlertStatus(id, status);
            result.put("success", true);
            result.put("message", "状态更新成功");
        } catch (Exception e) {
            log.error("更新状态失败", e);
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }
        return result;
    }
}

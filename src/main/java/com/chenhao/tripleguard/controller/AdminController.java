package com.chenhao.tripleguard.controller;

import com.chenhao.tripleguard.dto.AlertBatchDTO;
import com.chenhao.tripleguard.dto.AlertDTO;
import com.chenhao.tripleguard.dto.ApiResponse;
import com.chenhao.tripleguard.entity.Alert;
import com.chenhao.tripleguard.entity.AlertBatch;
import com.chenhao.tripleguard.repository.AlertBatchRepository;
import com.chenhao.tripleguard.repository.AlertRepository;
import com.chenhao.tripleguard.service.AlertAggregationService;
import com.chenhao.tripleguard.service.AlertService;
import com.chenhao.tripleguard.service.FeishuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository alertBatchRepository;
    private final AlertService alertService;
    private final AlertAggregationService aggregationService;
    private final FeishuNotificationService notificationService;

    // ==================== 统计信息 ====================

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 告警统计
        stats.put("totalAlerts", alertRepository.count());
        stats.put("firingAlerts", alertRepository.countByStatus("firing"));
        stats.put("resolvedAlerts", alertRepository.countByStatus("resolved"));

        // 批次统计
        stats.put("totalBatches", alertBatchRepository.count());
        stats.put("pendingBatches", alertBatchRepository.countByStatus("pending"));
        stats.put("processingBatches", alertBatchRepository.countByStatus("processing"));
        stats.put("resolvedBatches", alertBatchRepository.countByStatus("resolved"));

        // 严重级别统计
        stats.put("criticalBatches", alertBatchRepository.countBySeverity("critical"));
        stats.put("warningBatches", alertBatchRepository.countBySeverity("warning"));
        stats.put("infoBatches", alertBatchRepository.countBySeverity("info"));

        // 聚合状态
        stats.put("aggregationStatus", aggregationService.getAggregationStatus());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== 告警管理 ====================

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String source) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        Page<Alert> alertPage;

        if (status != null && severity != null) {
            alertPage = alertRepository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            alertPage = alertRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            alertPage = alertRepository.findBySeverity(severity, pageable);
        } else if (source != null) {
            alertPage = alertRepository.findBySource(source, pageable);
        } else {
            alertPage = alertRepository.findAll(pageable);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", alertPage.getContent().stream()
                .map(alertService::toDTO)
                .toList());
        result.put("totalElements", alertPage.getTotalElements());
        result.put("totalPages", alertPage.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", size);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<ApiResponse<AlertDTO>> getAlert(@PathVariable Long id) {
        return alertService.getAlertById(id)
                .map(alert -> ResponseEntity.ok(ApiResponse.success(alertService.toDTO(alert))))
                .orElse(ResponseEntity.ok(ApiResponse.badRequest("告警不存在")));
    }

    @GetMapping("/batches/{batchId}/alerts")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> getBatchAlerts(@PathVariable Long batchId) {
        List<Alert> alerts = alertRepository.findByBatchId(batchId);
        return ResponseEntity.ok(ApiResponse.success(alertService.toDTOList(alerts)));
    }

    // ==================== 批次管理 ====================

    @GetMapping("/batches")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AlertBatch> batchPage;

        if (status != null && severity != null) {
            batchPage = alertBatchRepository.findByStatusAndSeverity(status, severity, pageable);
        } else if (status != null) {
            batchPage = alertBatchRepository.findByStatus(status, pageable);
        } else if (severity != null) {
            batchPage = alertBatchRepository.findBySeverity(severity, pageable);
        } else {
            batchPage = alertBatchRepository.findAll(pageable);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("content", batchPage.getContent().stream()
                .map(this::toBatchDTO)
                .toList());
        result.put("totalElements", batchPage.getTotalElements());
        result.put("totalPages", batchPage.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", size);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<ApiResponse<AlertBatchDTO>> getBatch(@PathVariable Long id) {
        return alertBatchRepository.findById(id)
                .map(batch -> ResponseEntity.ok(ApiResponse.success(toBatchDTO(batch))))
                .orElse(ResponseEntity.ok(ApiResponse.badRequest("批次不存在")));
    }

    @PostMapping("/batches/{id}/notify")
    public ResponseEntity<ApiResponse<String>> resendNotification(@PathVariable Long id) {
        String webhookUrl = System.getenv("FEISHU_WEBHOOK_URL");
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.badRequest("飞书Webhook URL未配置"));
        }

        boolean success = notificationService.sendBatchNotification(id, webhookUrl);
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("通知发送成功"));
        } else {
            return ResponseEntity.ok(ApiResponse.badRequest("通知发送失败"));
        }
    }

    @PostMapping("/aggregation/flush")
    public ResponseEntity<ApiResponse<Map<String, Object>>> flushAggregation() {
        int count = aggregationService.getAggregationStatus() != null ?
                (int) aggregationService.getAggregationStatus().getOrDefault("activeGroups", 0) : 0;
        // 简化处理：返回当前状态
        Map<String, Object> result = new HashMap<>();
        result.put("flushed", count);
        result.put("status", aggregationService.getAggregationStatus());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 工具方法 ====================

    private AlertBatchDTO toBatchDTO(AlertBatch batch) {
        return AlertBatchDTO.builder()
                .id(batch.getId())
                .name(batch.getName())
                .status(batch.getStatus())
                .severity(batch.getSeverity())
                .alertCount(batch.getAlertCount())
                .uniqueCount(batch.getUniqueCount())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .escalatedAt(batch.getEscalatedAt())
                .resolvedAt(batch.getResolvedAt())
                .assignee(batch.getAssignee())
                .summary(batch.getSummary())
                .notes(batch.getNotes())
                .notificationStatus(batch.getNotificationStatus())
                .lastNotifiedAt(batch.getLastNotifiedAt())
                .build();
    }
}

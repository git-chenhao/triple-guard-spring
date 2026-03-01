package com.chenhao.tripleguard.controller;

import com.chenhao.tripleguard.dto.ApiResponse;
import com.chenhao.tripleguard.dto.AlertDTO;
import com.chenhao.tripleguard.entity.Alert;
import com.chenhao.tripleguard.service.AlertService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 告警Webhook接收控制器
 * <p>
 * 提供多种监控系统的Webhook接收端点，支持：
 * <ul>
 *   <li>Prometheus Alertmanager 格式</li>
 *   <li>Grafana 告警格式</li>
 *   <li>Zabbix 告警格式</li>
 * </ul>
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class AlertWebhookController {

    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    // ==================== Prometheus Alertmanager Webhook ====================

    /**
     * 接收 Prometheus Alertmanager 格式的告警
     * <p>
     * Alertmanager 发送的告警格式包含以下字段：
     * <ul>
     *   <li>status: firing 或 resolved</li>
     *   <li>alerts: 告警数组，每个告警包含 labels 和 annotations</li>
     * </ul>
     * </p>
     *
     * @param payload Prometheus Alertmanager 发送的JSON数据
     * @return API响应
     */
    @PostMapping("/alerts")
    public ResponseEntity<ApiResponse<List<AlertDTO>>> receivePrometheusAlert(
            @RequestBody JsonNode payload) {
        log.info("接收 Prometheus Alertmanager 告警");
        log.debug("原始数据: {}", payload);

        try {
            List<AlertDTO> receivedAlerts = new ArrayList<>();

            // 解析 Alertmanager 格式
            String status = payload.has("status") ? payload.get("status").asText() : "firing";
            JsonNode alertsNode = payload.get("alerts");

            if (alertsNode != null && alertsNode.isArray()) {
                for (JsonNode alertNode : alertsNode) {
                    Alert alert = parsePrometheusAlert(alertNode, status);
                    Alert saved = alertService.receiveAlert(
                            alert.getAlertname(),
                            alert.getSeverity(),
                            alert.getStatus(),
                            alert.getLabels(),
                            alert.getAnnotations(),
                            "prometheus",
                            alertNode.toString()
                    );
                    receivedAlerts.add(alertService.toDTO(saved));
                }
            }

            log.info("成功接收 {} 条 Prometheus 告警", receivedAlerts.size());
            return ResponseEntity.ok(ApiResponse.success(receivedAlerts,
                    "成功接收 " + receivedAlerts.size() + " 条告警"));

        } catch (Exception e) {
            log.error("处理 Prometheus 告警失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("处理告警失败: " + e.getMessage()));
        }
    }

    /**
     * 解析单个 Prometheus 告警
     *
     * @param alertNode 告警JSON节点
     * @param overallStatus 整体状态
     * @return 告警实体
     */
    private Alert parsePrometheusAlert(JsonNode alertNode, String overallStatus) {
        Map<String, String> labels = new HashMap<>();
        Map<String, String> annotations = new HashMap<>();

        // 解析 labels
        JsonNode labelsNode = alertNode.get("labels");
        if (labelsNode != null) {
            labelsNode.fields().forEachRemaining(entry ->
                    labels.put(entry.getKey(), entry.getValue().asText()));
        }

        // 解析 annotations
        JsonNode annotationsNode = alertNode.get("annotations");
        if (annotationsNode != null) {
            annotationsNode.fields().forEachRemaining(entry ->
                    annotations.put(entry.getKey(), entry.getValue().asText()));
        }

        String alertname = labels.getOrDefault("alertname", "unknown");
        String severity = labels.getOrDefault("severity", "warning");
        String status = alertNode.has("status") ?
                alertNode.get("status").asText() : overallStatus;

        return Alert.builder()
                .alertname(alertname)
                .severity(severity)
                .status(status)
                .labels(labels)
                .annotations(annotations)
                .build();
    }

    // ==================== Grafana Webhook ====================

    /**
     * 接收 Grafana 格式的告警
     * <p>
     * Grafana 发送的告警格式包含以下字段：
     * <ul>
     *   <li>title: 告警标题</li>
     *   <li>message: 告警消息</li>
     *   <li>state: 告警状态 (alerting/ok)</li>
     *   <li>tags: 标签集合</li>
     * </ul>
     * </p>
     *
     * @param payload Grafana 发送的JSON数据
     * @return API响应
     */
    @PostMapping("/grafana")
    public ResponseEntity<ApiResponse<AlertDTO>> receiveGrafanaAlert(
            @RequestBody JsonNode payload) {
        log.info("接收 Grafana 告警");
        log.debug("原始数据: {}", payload);

        try {
            String title = payload.has("title") ? payload.get("title").asText() : "Grafana Alert";
            String message = payload.has("message") ? payload.get("message").asText() : "";
            String state = payload.has("state") ? payload.get("state").asText() : "alerting";

            // 转换状态
            String status = "alerting".equals(state) || "alerting".equalsIgnoreCase(state) ? "firing" : "resolved";

            // 解析标签
            Map<String, String> labels = new HashMap<>();
            JsonNode tagsNode = payload.get("tags");
            if (tagsNode != null) {
                tagsNode.fields().forEachRemaining(entry ->
                        labels.put(entry.getKey(), entry.getValue().asText()));
            }
            labels.put("source", "grafana");

            // 解析严重级别
            String severity = labels.getOrDefault("severity", "warning");

            // 构建注解
            Map<String, String> annotations = new HashMap<>();
            annotations.put("message", message);
            annotations.put("summary", title);

            Alert alert = alertService.receiveAlert(
                    title,
                    severity,
                    status,
                    labels,
                    annotations,
                    "grafana",
                    payload.toString()
            );

            log.info("成功接收 Grafana 告警: {}", title);
            return ResponseEntity.ok(ApiResponse.success(alertService.toDTO(alert),
                    "成功接收 Grafana 告警"));

        } catch (Exception e) {
            log.error("处理 Grafana 告警失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("处理告警失败: " + e.getMessage()));
        }
    }

    // ==================== Zabbix Webhook ====================

    /**
     * 接收 Zabbix 格式的告警
     * <p>
     * Zabbix 通过媒介类型配置发送告警，支持自定义JSON格式。
     * 推荐的字段包括：
     * <ul>
     *   <li>host: 主机名</li>
     *   <li>trigger: 触发器名称</li>
     *   <li>severity: 严重级别</li>
     *   <li>message: 告警消息</li>
     * </ul>
     * </p>
     *
     * @param payload Zabbix 发送的JSON数据
     * @return API响应
     */
    @PostMapping("/zabbix")
    public ResponseEntity<ApiResponse<AlertDTO>> receiveZabbixAlert(
            @RequestBody JsonNode payload) {
        log.info("接收 Zabbix 告警");
        log.debug("原始数据: {}", payload);

        try {
            // 解析 Zabbix 格式（支持多种格式）
            String host = payload.has("host") ? payload.get("host").asText() : "unknown";
            String trigger = payload.has("trigger") ? payload.get("trigger").asText() :
                    (payload.has("name") ? payload.get("name").asText() : "Zabbix Alert");
            String message = payload.has("message") ? payload.get("message").asText() : "";
            String zabbixSeverity = payload.has("severity") ? payload.get("severity").asText() : "Warning";

            // 转换严重级别
            String severity = convertZabbixSeverity(zabbixSeverity);

            // 构建标签
            Map<String, String> labels = new HashMap<>();
            labels.put("host", host);
            labels.put("source", "zabbix");
            if (payload.has("item")) {
                labels.put("item", payload.get("item").asText());
            }
            if (payload.has("eventid")) {
                labels.put("eventid", payload.get("eventid").asText());
            }

            // 构建注解
            Map<String, String> annotations = new HashMap<>();
            annotations.put("message", message);
            annotations.put("trigger", trigger);

            // Zabbix 通常没有 resolved 状态，默认为 firing
            String status = payload.has("status") ? payload.get("status").asText() : "firing";
            if ("OK".equalsIgnoreCase(status) || "0".equals(status)) {
                status = "resolved";
            } else {
                status = "firing";
            }

            Alert alert = alertService.receiveAlert(
                    trigger,
                    severity,
                    status,
                    labels,
                    annotations,
                    "zabbix",
                    payload.toString()
            );

            log.info("成功接收 Zabbix 告警: {} - {}", host, trigger);
            return ResponseEntity.ok(ApiResponse.success(alertService.toDTO(alert),
                    "成功接收 Zabbix 告警"));

        } catch (Exception e) {
            log.error("处理 Zabbix 告警失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("处理告警失败: " + e.getMessage()));
        }
    }

    /**
     * 转换 Zabbix 严重级别到系统级别
     *
     * @param zabbixSeverity Zabbix 严重级别
     * @return 系统严重级别
     */
    private String convertZabbixSeverity(String zabbixSeverity) {
        if (zabbixSeverity == null) {
            return "warning";
        }

        return switch (zabbixSeverity.toLowerCase()) {
            case "disaster", "high", "4", "5" -> "critical";
            case "average", "warning", "2", "3" -> "warning";
            case "information", "not classified", "0", "1" -> "info";
            default -> "warning";
        };
    }

    // ==================== 通用告警接口 ====================

    /**
     * 创建告警（手动创建）
     * <p>
     * 用于手动创建告警或测试目的
     * </p>
     *
     * @param alertDTO 告警DTO
     * @return API响应
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AlertDTO>> createAlert(@Valid @RequestBody AlertDTO alertDTO) {
        log.info("手动创建告警: {}", alertDTO.getAlertname());

        try {
            Alert alert = alertService.createAlert(alertDTO);
            return ResponseEntity.ok(ApiResponse.success(alertService.toDTO(alert),
                    "告警创建成功"));
        } catch (Exception e) {
            log.error("创建告警失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("创建告警失败: " + e.getMessage()));
        }
    }

    // ==================== 健康检查 ====================

    /**
     * Webhook 健康检查端点
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("supportedSources", List.of("prometheus", "grafana", "zabbix"));

        return ResponseEntity.ok(ApiResponse.success(health, "Webhook服务正常"));
    }
}

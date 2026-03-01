package com.chenhao.tripleguard.service;

import com.chenhao.tripleguard.config.AlertConfig;
import com.chenhao.tripleguard.entity.Alert;
import com.chenhao.tripleguard.entity.AlertBatch;
import com.chenhao.tripleguard.repository.AlertBatchRepository;
import com.chenhao.tripleguard.repository.AlertRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A2A调度服务
 * <p>
 * 负责调用 Sub-Agent 进行根因分析，支持 HTTP 和 gRPC 调用方式。
 * 当告警批次创建后，自动调度分析Agent对批次进行根因分析。
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSchedulerService {

    private final AlertBatchRepository alertBatchRepository;
    private final AlertRepository alertRepository;
    private final AlertConfig alertConfig;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Agent服务端点配置
     * <p>
     * 通过环境变量配置：AGENT_ANALYSIS_ENDPOINT
     * </p>
     */
    private static final String AGENT_ENDPOINT_ENV = "AGENT_ANALYSIS_ENDPOINT";

    /**
     * 调度批次进行根因分析
     * <p>
     * 将批次信息发送到分析Agent，获取根因分析结果
     * </p>
     *
     * @param batchId 批次ID
     * @return 分析是否成功
     */
    @Transactional
    public boolean scheduleAnalysis(Long batchId) {
        log.info("调度批次分析: batchId={}", batchId);

        AlertBatch batch = alertBatchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            log.warn("批次不存在: batchId={}", batchId);
            return false;
        }

        String agentEndpoint = System.getenv(AGENT_ENDPOINT_ENV);
        if (agentEndpoint == null || agentEndpoint.isEmpty()) {
            log.warn("Agent分析端点未配置，跳过分析");
            batch.setNotes("Agent端点未配置，跳过分析");
            alertBatchRepository.save(batch);
            return false;
        }

        try {
            // 准备分析请求
            String analysisRequest = buildAnalysisRequest(batch);

            // 调用Agent服务
            String analysisResult = callAgentService(agentEndpoint, analysisRequest);

            // 处理分析结果
            return handleAnalysisResult(batch, analysisResult);

        } catch (Exception e) {
            log.error("调度分析失败: batchId={}", batchId, e);
            batch.setNotes("分析失败: " + e.getMessage());
            alertBatchRepository.save(batch);
            return false;
        }
    }

    /**
     * 构建分析请求
     *
     * @param batch 告警批次
     * @return JSON请求字符串
     */
    private String buildAnalysisRequest(AlertBatch batch) {
        List<Alert> alerts = alertRepository.findByBatchId(batch.getId());

        ObjectNode request = objectMapper.createObjectNode();
        request.put("batch_id", batch.getId());
        request.put("batch_name", batch.getName());
        request.put("severity", batch.getSeverity());
        request.put("alert_count", batch.getAlertCount());
        request.put("created_at", batch.getCreatedAt().format(DATETIME_FORMATTER));

        // 添加告警详情
        var alertsArray = request.putArray("alerts");
        for (Alert alert : alerts) {
            ObjectNode alertNode = alertsArray.addObject();
            alertNode.put("alertname", alert.getAlertname());
            alertNode.put("severity", alert.getSeverity());
            alertNode.put("fingerprint", alert.getFingerprint());
            alertNode.put("received_at", alert.getReceivedAt().format(DATETIME_FORMATTER));

            // 添加标签
            if (alert.getLabels() != null) {
                ObjectNode labelsNode = alertNode.putObject("labels");
                alert.getLabels().forEach(labelsNode::put);
            }

            // 添加注解
            if (alert.getAnnotations() != null) {
                ObjectNode annotationsNode = alertNode.putObject("annotations");
                alert.getAnnotations().forEach(annotationsNode::put);
            }
        }

        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("构建分析请求失败", e);
            return "{}";
        }
    }

    /**
     * 调用Agent服务
     *
     * @param endpoint Agent服务端点
     * @param request  请求JSON
     * @return 响应JSON
     */
    private String callAgentService(String endpoint, String request) throws IOException {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                request
        );

        Request httpRequest = new Request.Builder()
                .url(endpoint)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", "TripleGuard/1.0")
                .build();

        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            throw new IOException("Empty response from agent service");
        }
    }

    /**
     * 处理分析结果
     *
     * @param batch   告警批次
     * @param result  分析结果JSON
     * @return 处理是否成功
     */
    private boolean handleAnalysisResult(AlertBatch batch, String result) {
        try {
            JsonNode resultJson = objectMapper.readTree(result);

            // 检查分析是否成功
            if (resultJson.has("success") && resultJson.get("success").asBoolean()) {
                // 提取根因分析结果
                String rootCause = resultJson.has("root_cause")
                        ? resultJson.get("root_cause").asText()
                        : "未识别到根因";

                String recommendations = resultJson.has("recommendations")
                        ? resultJson.get("recommendations").asText()
                        : "无建议";

                // 更新批次信息
                batch.setNotes(String.format("根因分析结果:\n%s\n\n建议:\n%s",
                        rootCause, recommendations));
                batch.setStatus("escalated");
                batch.setEscalatedAt(LocalDateTime.now());
                alertBatchRepository.save(batch);

                log.info("批次分析成功: batchId={}, rootCause={}", batch.getId(), rootCause);
                return true;
            } else {
                String errorMsg = resultJson.has("error")
                        ? resultJson.get("error").asText()
                        : "未知错误";
                batch.setNotes("Agent分析失败: " + errorMsg);
                alertBatchRepository.save(batch);
                log.warn("Agent分析失败: batchId={}, error={}", batch.getId(), errorMsg);
                return false;
            }

        } catch (Exception e) {
            log.error("处理分析结果失败: batchId={}", batch.getId(), e);
            batch.setNotes("处理分析结果异常: " + e.getMessage());
            alertBatchRepository.save(batch);
            return false;
        }
    }

    /**
     * 定时检查待分析的批次
     * <p>
     * 每30秒检查一次状态为processing的批次，触发根因分析
     * </p>
     */
    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void schedulePendingAnalyses() {
        if (!alertConfig.getAggregation().isEnabled()) {
            return;
        }

        List<AlertBatch> processingBatches = alertBatchRepository.findByStatus("processing");

        if (processingBatches.isEmpty()) {
            return;
        }

        log.info("发现 {} 个待分析批次", processingBatches.size());

        for (AlertBatch batch : processingBatches) {
            try {
                scheduleAnalysis(batch.getId());
            } catch (Exception e) {
                log.error("调度分析失败: batchId={}", batch.getId(), e);
            }
        }
    }

    /**
     * 手动触发批次分析
     *
     * @param batchId 批次ID
     * @return 分析是否成功
     */
    @Transactional
    public boolean triggerAnalysis(Long batchId) {
        log.info("手动触发分析: batchId={}", batchId);

        AlertBatch batch = alertBatchRepository.findById(batchId).orElse(null);
        if (batch == null) {
            log.warn("批次不存在: batchId={}", batchId);
            return false;
        }

        // 更新批次状态
        batch.setStatus("processing");
        batch.setUpdatedAt(LocalDateTime.now());
        alertBatchRepository.save(batch);

        return scheduleAnalysis(batchId);
    }

    /**
     * 获取分析统计信息
     *
     * @return 统计信息Map
     */
    public Map<String, Object> getAnalysisStats() {
        return Map.of(
                "processingBatches", alertBatchRepository.countByStatus("processing"),
                "escalatedBatches", alertBatchRepository.countByStatus("escalated"),
                "agentEndpoint", System.getenv(AGENT_ENDPOINT_ENV) != null
                        ? "已配置" : "未配置"
        );
    }
}

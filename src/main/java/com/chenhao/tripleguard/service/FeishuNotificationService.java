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
import java.util.concurrent.TimeUnit;

/**
 * 飞书通知服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuNotificationService {

    private final AlertBatchRepository alertBatchRepository;
    private final AlertRepository alertRepository;
    private final AlertConfig alertConfig;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .build();

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean sendBatchNotification(Long batchId, String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("飞书Webhook URL未配置");
            return false;
        }

        try {
            AlertBatch batch = alertBatchRepository.findById(batchId).orElse(null);
            if (batch == null) {
                return false;
            }

            List<Alert> alerts = alertRepository.findByBatchId(batchId);
            String cardContent = buildCardContent(batch, alerts);
            return sendToFeishu(webhookUrl, cardContent);

        } catch (Exception e) {
            log.error("发送飞书通知失败: batchId={}", batchId, e);
            return false;
        }
    }

    private String buildCardContent(AlertBatch batch, List<Alert> alerts) {
        ObjectNode card = objectMapper.createObjectNode();
        card.put("msg_type", "interactive");

        ObjectNode cardContent = objectMapper.createObjectNode();

        // 卡片头部
        ObjectNode header = objectMapper.createObjectNode();
        header.put("title", buildPlainText(getSeverityTitle(batch.getSeverity())));
        header.put("template", getSeverityColor(batch.getSeverity()));
        cardContent.set("header", header);

        // 卡片元素
        var elements = objectMapper.createArrayNode();

        // 批次信息
        elements.add(buildMarkdownElement(
                "**批次名称**: " + batch.getName() + "\n" +
                "**状态**: " + batch.getStatus() + "\n" +
                "**严重级别**: " + batch.getSeverity() + "\n" +
                "**告警数量**: " + batch.getAlertCount() + " 条（" + batch.getUniqueCount() + " 条唯一）\n" +
                "**创建时间**: " + batch.getCreatedAt().format(DATETIME_FORMATTER)
        ));

        cardContent.set("elements", elements);
        card.set("card", cardContent);

        try {
            return objectMapper.writeValueAsString(card);
        } catch (Exception e) {
            log.error("构建卡片内容失败", e);
            return "{}";
        }
    }

    private ObjectNode buildPlainText(String text) {
        ObjectNode textNode = objectMapper.createObjectNode();
        textNode.put("tag", "plain_text");
        textNode.put("content", text);
        return textNode;
    }

    private ObjectNode buildMarkdownElement(String content) {
        ObjectNode element = objectMapper.createObjectNode();
        element.put("tag", "div");
        element.set("text", buildMarkdownText(content));
        return element;
    }

    private ObjectNode buildMarkdownText(String content) {
        ObjectNode textNode = objectMapper.createObjectNode();
        textNode.put("tag", "lark_md");
        textNode.put("content", content);
        return textNode;
    }

    private String getSeverityTitle(String severity) {
        return switch (severity) {
            case "critical" -> "🔴 严重告警";
            case "warning" -> "🟡 警告告警";
            case "info" -> "🔵 信息告警";
            default -> "告警通知";
        };
    }

    private String getSeverityColor(String severity) {
        return switch (severity) {
            case "critical" -> "red";
            case "warning" -> "yellow";
            case "info" -> "blue";
            default -> "grey";
        };
    }

    private boolean sendToFeishu(String webhookUrl, String content) {
        try {
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    content
            );

            Request request = new Request.Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    if (jsonNode.has("code") && jsonNode.get("code").asInt() == 0) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            log.error("飞书通知IO异常", e);
        }
        return false;
    }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void processPendingNotifications() {
        if (!alertConfig.getNotification().isEnabled()) {
            return;
        }

        List<AlertBatch> pendingBatches = alertBatchRepository.findByNotificationStatus("pending");

        for (AlertBatch batch : pendingBatches) {
            try {
                String webhookUrl = System.getenv("FEISHU_WEBHOOK_URL");
                if (webhookUrl == null || webhookUrl.isEmpty()) {
                    batch.setNotificationStatus("failed");
                    alertBatchRepository.save(batch);
                    continue;
                }

                boolean success = sendBatchNotification(batch.getId(), webhookUrl);

                if (success) {
                    batch.setNotificationStatus("sent");
                    batch.setLastNotifiedAt(LocalDateTime.now());
                } else {
                    batch.setNotificationStatus("failed");
                }

                alertBatchRepository.save(batch);

            } catch (Exception e) {
                log.error("处理批次通知失败: batchId={}", batch.getId(), e);
            }
        }
    }
}

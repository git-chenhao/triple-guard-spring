package com.chenhao.tripleguard.service;

import com.chenhao.tripleguard.config.AlertConfig;
import com.chenhao.tripleguard.entity.Alert;
import com.chenhao.tripleguard.entity.AlertBatch;
import com.chenhao.tripleguard.repository.AlertBatchRepository;
import com.chenhao.tripleguard.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 告警聚合服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertAggregationService {

    private final AlertRepository alertRepository;
    private final AlertBatchRepository alertBatchRepository;
    private final AlertConfig alertConfig;
    private final FeishuNotificationService feishuNotificationService;

    private final ConcurrentHashMap<String, AggregationGroup> aggregationGroups = new ConcurrentHashMap<>();

    private static class AggregationGroup {
        String groupKey;
        String batchName;
        LocalDateTime firstAlertTime;
        Set<Long> alertIds = new HashSet<>();
        String severity = "info";

        AggregationGroup(String groupKey, String batchName, LocalDateTime firstAlertTime) {
            this.groupKey = groupKey;
            this.batchName = batchName;
            this.firstAlertTime = firstAlertTime;
        }
    }

    @Transactional
    public void addToAggregation(Alert alert) {
        if (!alertConfig.getAggregation().isEnabled()) {
            return;
        }

        String groupKey = generateGroupKey(alert);
        String batchName = generateBatchName(alert);

        AggregationGroup group = aggregationGroups.computeIfAbsent(groupKey,
                k -> new AggregationGroup(groupKey, batchName, LocalDateTime.now()));

        synchronized (group) {
            group.alertIds.add(alert.getId());
            group.severity = getHigherSeverity(group.severity, alert.getSeverity());

            if (group.alertIds.size() >= alertConfig.getAggregation().getMaxBatchSize()) {
                flushGroup(group);
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void scheduledFlush() {
        if (!alertConfig.getAggregation().isEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<AggregationGroup> groupsToFlush = new ArrayList<>();

        aggregationGroups.forEach((key, group) -> {
            synchronized (group) {
                long elapsedSeconds = java.time.Duration.between(
                        group.firstAlertTime, now).getSeconds();

                if (elapsedSeconds >= alertConfig.getAggregation().getTimeWindowSeconds()) {
                    groupsToFlush.add(group);
                }
            }
        });

        for (AggregationGroup group : groupsToFlush) {
            flushGroup(group);
        }
    }

    @Transactional
    public void flushGroup(AggregationGroup group) {
        if (group == null) {
            return;
        }

        synchronized (group) {
            try {
                aggregationGroups.remove(group.groupKey);

                if (group.alertIds.isEmpty()) {
                    return;
                }

                List<Alert> alerts = alertRepository.findAllById(group.alertIds);

                if (alerts.isEmpty()) {
                    return;
                }

                long uniqueCount = alerts.stream()
                        .map(Alert::getFingerprint)
                        .distinct()
                        .count();

                AlertBatch batch = AlertBatch.builder()
                        .name(group.batchName)
                        .status("pending")
                        .severity(group.severity)
                        .alertCount(alerts.size())
                        .uniqueCount((int) uniqueCount)
                        .createdAt(LocalDateTime.now())
                        .notificationStatus("pending")
                        .summary(generateBatchSummary(alerts))
                        .build();

                alertBatchRepository.save(batch);

                List<Long> alertIds = new ArrayList<>(group.alertIds);
                alertRepository.updateBatchIdByIds(alertIds, batch.getId());

                log.info("创建告警批次: id={}, name={}, alertCount={}",
                        batch.getId(), batch.getName(), batch.getAlertCount());

            } catch (Exception e) {
                log.error("聚合组处理失败: groupKey={}", group.groupKey, e);
            }
        }
    }

    private String generateGroupKey(Alert alert) {
        return alert.getAlertname() + "|" + alert.getSeverity() + "|" + alert.getSource();
    }

    private String generateBatchName(Alert alert) {
        return String.format("[%s] %s", alert.getSeverity(), alert.getAlertname());
    }

    private String generateBatchSummary(List<Alert> alerts) {
        if (alerts.isEmpty()) {
            return "无告警";
        }
        return String.format("共 %d 条告警（%d 条唯一），主要告警：%s",
                alerts.size(),
                alerts.stream().map(Alert::getFingerprint).distinct().count(),
                alerts.get(0).getAlertname());
    }

    private String getHigherSeverity(String current, String newLevel) {
        if (newLevel == null) {
            return current;
        }
        if ("critical".equals(newLevel)) {
            return "critical";
        } else if ("warning".equals(newLevel)) {
            return "critical".equals(current) ? "critical" : "warning";
        }
        return current;
    }

    public Map<String, Object> getAggregationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("activeGroups", aggregationGroups.size());
        return status;
    }
}

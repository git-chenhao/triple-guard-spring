package com.chenhao.tripleguard.service;

import com.chenhao.tripleguard.dto.AlertDTO;
import com.chenhao.tripleguard.entity.Alert;
import com.chenhao.tripleguard.repository.AlertRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 告警业务逻辑服务
 * <p>
 * 提供告警的接收、处理、查询和管理功能，包括：
 * <ul>
 *   <li>告警接收与指纹生成</li>
 *   <li>告警CRUD操作</li>
 *   <li>告警查询与统计</li>
 * </ul>
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final ObjectMapper objectMapper;

    /**
     * 接收并处理告警
     * <p>
     * 接收告警数据，生成指纹，检查是否为重复告警，
     * 如果是重复告警则更新状态，否则创建新告警记录
     * </p>
     *
     * @param alertName  告警名称
     * @param severity   严重级别 (critical/warning/info)
     * @param status     告警状态 (firing/resolved)
     * @param labels     标签集合
     * @param annotations 注解集合
     * @param source     告警来源
     * @param rawJson    原始JSON数据
     * @return 保存后的告警实体
     */
    @Transactional
    public Alert receiveAlert(String alertName, String severity, String status,
                              Map<String, String> labels, Map<String, String> annotations,
                              String source, String rawJson) {
        log.info("接收告警: name={}, severity={}, source={}", alertName, severity, source);

        // 生成告警指纹
        String fingerprint = generateFingerprint(alertName, labels);
        log.debug("生成告警指纹: {}", fingerprint);

        // 检查是否存在相同指纹的告警
        Optional<Alert> existingAlert = alertRepository.findByFingerprint(fingerprint);

        Alert alert;
        if (existingAlert.isPresent()) {
            // 更新现有告警状态
            alert = existingAlert.get();
            alert.setStatus(status);
            alert.setSeverity(severity);
            alert.setLabels(labels);
            alert.setAnnotations(annotations);
            alert.setReceivedAt(LocalDateTime.now());
            if (rawJson != null) {
                alert.setRawJson(rawJson);
            }
            log.info("更新已存在的告警: id={}, fingerprint={}", alert.getId(), fingerprint);
        } else {
            // 创建新告警
            alert = Alert.builder()
                    .fingerprint(fingerprint)
                    .alertname(alertName)
                    .severity(severity != null ? severity : "warning")
                    .status(status != null ? status : "firing")
                    .labels(labels)
                    .annotations(annotations)
                    .source(source)
                    .receivedAt(LocalDateTime.now())
                    .rawJson(rawJson)
                    .build();
            log.info("创建新告警: fingerprint={}", fingerprint);
        }

        return alertRepository.save(alert);
    }

    /**
     * 生成告警指纹
     * <p>
     * 基于告警名称和关键标签生成MD5哈希值，用于告警去重。
     * 关键标签包括：alertname, instance, job, severity
     * </p>
     *
     * @param alertName 告警名称
     * @param labels    标签集合
     * @return MD5哈希值（32位十六进制字符串）
     */
    public String generateFingerprint(String alertName, Map<String, String> labels) {
        StringBuilder sb = new StringBuilder();
        sb.append("alertname:").append(alertName);

        // 添加关键标签用于生成指纹
        if (labels != null) {
            // 按字母顺序排序标签键，确保指纹稳定性
            List<String> sortedKeys = labels.keySet().stream()
                    .sorted()
                    .toList();

            for (String key : sortedKeys) {
                sb.append("|").append(key).append(":").append(labels.get(key));
            }
        }

        return md5Hash(sb.toString());
    }

    /**
     * 计算MD5哈希值
     *
     * @param input 输入字符串
     * @return MD5哈希值（32位十六进制字符串）
     */
    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5算法不可用", e);
            throw new RuntimeException("MD5算法不可用", e);
        }
    }

    // ==================== CRUD 操作 ====================

    /**
     * 创建新告警
     *
     * @param alertDTO 告警DTO
     * @return 创建的告警实体
     */
    @Transactional
    public Alert createAlert(AlertDTO alertDTO) {
        String fingerprint = generateFingerprint(alertDTO.getAlertname(), alertDTO.getLabels());

        Alert alert = Alert.builder()
                .fingerprint(fingerprint)
                .alertname(alertDTO.getAlertname())
                .severity(alertDTO.getSeverity() != null ? alertDTO.getSeverity() : "warning")
                .status(alertDTO.getStatus() != null ? alertDTO.getStatus() : "firing")
                .labels(alertDTO.getLabels())
                .annotations(alertDTO.getAnnotations())
                .source(alertDTO.getSource())
                .receivedAt(LocalDateTime.now())
                .summary(alertDTO.getSummary())
                .description(alertDTO.getDescription())
                .build();

        return alertRepository.save(alert);
    }

    /**
     * 根据ID获取告警
     *
     * @param id 告警ID
     * @return 告警实体（可能为空）
     */
    public Optional<Alert> getAlertById(Long id) {
        return alertRepository.findById(id);
    }

    /**
     * 根据指纹获取告警
     *
     * @param fingerprint 告警指纹
     * @return 告警实体（可能为空）
     */
    public Optional<Alert> getAlertByFingerprint(String fingerprint) {
        return alertRepository.findByFingerprint(fingerprint);
    }

    /**
     * 获取所有告警（分页）
     *
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    public Page<Alert> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    /**
     * 更新告警
     *
     * @param id       告警ID
     * @param alertDTO 告警DTO
     * @return 更新后的告警实体
     */
    @Transactional
    public Alert updateAlert(Long id, AlertDTO alertDTO) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("告警不存在: " + id));

        if (alertDTO.getAlertname() != null) {
            alert.setAlertname(alertDTO.getAlertname());
        }
        if (alertDTO.getSeverity() != null) {
            alert.setSeverity(alertDTO.getSeverity());
        }
        if (alertDTO.getStatus() != null) {
            alert.setStatus(alertDTO.getStatus());
        }
        if (alertDTO.getLabels() != null) {
            alert.setLabels(alertDTO.getLabels());
        }
        if (alertDTO.getAnnotations() != null) {
            alert.setAnnotations(alertDTO.getAnnotations());
        }
        if (alertDTO.getSource() != null) {
            alert.setSource(alertDTO.getSource());
        }
        if (alertDTO.getSummary() != null) {
            alert.setSummary(alertDTO.getSummary());
        }
        if (alertDTO.getDescription() != null) {
            alert.setDescription(alertDTO.getDescription());
        }
        if (alertDTO.getBatchId() != null) {
            alert.setBatchId(alertDTO.getBatchId());
        }

        return alertRepository.save(alert);
    }

    /**
     * 删除告警
     *
     * @param id 告警ID
     */
    @Transactional
    public void deleteAlert(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new IllegalArgumentException("告警不存在: " + id);
        }
        alertRepository.deleteById(id);
        log.info("删除告警: id={}", id);
    }

    // ==================== 查询操作 ====================

    /**
     * 根据状态查询告警
     *
     * @param status   告警状态
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    public Page<Alert> getAlertsByStatus(String status, Pageable pageable) {
        return alertRepository.findByStatus(status, pageable);
    }

    /**
     * 根据来源查询告警
     *
     * @param source   告警来源
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    public Page<Alert> getAlertsBySource(String source, Pageable pageable) {
        return alertRepository.findBySource(source, pageable);
    }

    /**
     * 根据严重级别查询告警
     *
     * @param severity 严重级别
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    public Page<Alert> getAlertsBySeverity(String severity, Pageable pageable) {
        return alertRepository.findBySeverity(severity, pageable);
    }

    /**
     * 根据批次ID查询告警
     *
     * @param batchId 批次ID
     * @return 告警列表
     */
    public List<Alert> getAlertsByBatchId(Long batchId) {
        return alertRepository.findByBatchId(batchId);
    }

    /**
     * 获取未分配批次的告警
     *
     * @return 未分配批次的告警列表
     */
    public List<Alert> getUnbatchedAlerts() {
        return alertRepository.findUnbatchedAlerts();
    }

    /**
     * 统计指定状态的告警数量
     *
     * @param status 告警状态
     * @return 告警数量
     */
    public long countByStatus(String status) {
        return alertRepository.countByStatus(status);
    }

    /**
     * 检查告警是否存在
     *
     * @param id 告警ID
     * @return 是否存在
     */
    public boolean existsById(Long id) {
        return alertRepository.existsById(id);
    }

    // ==================== DTO 转换 ====================

    /**
     * 将实体转换为DTO
     *
     * @param alert 告警实体
     * @return 告警DTO
     */
    public AlertDTO toDTO(Alert alert) {
        return AlertDTO.builder()
                .id(alert.getId())
                .fingerprint(alert.getFingerprint())
                .alertname(alert.getAlertname())
                .severity(alert.getSeverity())
                .status(alert.getStatus())
                .labels(alert.getLabels())
                .annotations(alert.getAnnotations())
                .source(alert.getSource())
                .receivedAt(alert.getReceivedAt())
                .batchId(alert.getBatchId())
                .summary(alert.getSummary())
                .description(alert.getDescription())
                .build();
    }

    /**
     * 批量将实体转换为DTO
     *
     * @param alerts 告警实体列表
     * @return 告警DTO列表
     */
    public List<AlertDTO> toDTOList(List<Alert> alerts) {
        return alerts.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将分页实体转换为分页DTO
     *
     * @param alertPage 分页告警实体
     * @return 分页告警DTO
     */
    public Page<AlertDTO> toDTOPage(Page<Alert> alertPage) {
        return alertPage.map(this::toDTO);
    }
}

package com.chenhao.tripleguard.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警实体类
 * <p>
 * 用于存储接收到的告警信息，支持多种告警系统格式
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_fingerprint", columnList = "fingerprint"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_batch_id", columnList = "batch_id"),
        @Index(name = "idx_received_at", columnList = "received_at")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Alert {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警指纹
     * <p>
     * 基于告警名称和关键标签生成的MD5哈希值，用于告警去重
     * </p>
     */
    @Column(name = "fingerprint", length = 32, nullable = false)
    private String fingerprint;

    /**
     * 告警名称
     */
    @Column(name = "alertname", nullable = false, length = 255)
    private String alertname;

    /**
     * 严重级别
     * <p>
     * 可选值: critical, warning, info
     * </p>
     */
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * 告警状态
     * <p>
     * 可选值: firing, resolved
     * </p>
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 标签集合
     * <p>
     * 键值对形式的附加标签信息，用于告警分类和筛选
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "labels", columnDefinition = "JSON")
    private Map<String, String> labels;

    /**
     * 注解集合
     * <p>
     * 键值对形式的附加描述信息
     * </p>
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "annotations", columnDefinition = "JSON")
    private Map<String, String> annotations;

    /**
     * 告警来源
     * <p>
     * 例如: prometheus, grafana, zabbix
     * </p>
     */
    @Column(name = "source", length = 50)
    private String source;

    /**
     * 接收时间
     */
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * 所属批次ID
     * <p>
     * 用于关联聚合批次，null表示尚未分配到批次
     * </p>
     */
    @Column(name = "batch_id")
    private Long batchId;

    /**
     * 告警摘要
     */
    @Column(name = "summary", length = 500)
    private String summary;

    /**
     * 告警描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 原始告警JSON
     * <p>
     * 保存原始告警数据，便于后续分析
     * </p>
     */
    @Column(name = "raw_json", columnDefinition = "TEXT")
    private String rawJson;
}

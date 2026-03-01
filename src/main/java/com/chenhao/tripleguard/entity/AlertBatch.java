package com.chenhao.tripleguard.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警批次实体类
 * <p>
 * 用于存储聚合后的告警批次信息，每个批次包含一段时间内
 * 相似或相关告警的聚合统计
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
@Table(name = "alert_batches", indexes = {
        @Index(name = "idx_batch_status", columnList = "status"),
        @Index(name = "idx_batch_created_at", columnList = "created_at"),
        @Index(name = "idx_batch_escalated_at", columnList = "escalated_at")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertBatch {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 批次名称
     * <p>
     * 基于告警特征生成的批次标识名称
     * </p>
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 批次状态
     * <p>
     * 可选值: pending（待处理）, processing（处理中）,
     * escalated（已升级）, resolved（已解决）, closed（已关闭）
     * </p>
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /**
     * 严重级别
     * <p>
     * 批次内最高严重级别，可选值: critical, warning, info
     * </p>
     */
    @Column(name = "severity", length = 20)
    private String severity;

    /**
     * 批次内告警数量
     */
    @Column(name = "alert_count", nullable = false)
    private Integer alertCount;

    /**
     * 批次内唯一告警数（按fingerprint去重）
     */
    @Column(name = "unique_count", nullable = false)
    private Integer uniqueCount;

    /**
     * 批次创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 升级时间
     * <p>
     * 批次被标记为escalated的时间
     * </p>
     */
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    /**
     * 解决时间
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /**
     * 处理人
     * <p>
     * 分配处理该批次的责任人
     * </p>
     */
    @Column(name = "assignee", length = 100)
    private String assignee;

    /**
     * 批次摘要
     */
    @Column(name = "summary", length = 500)
    private String summary;

    /**
     * 批次备注
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * 通知状态
     * <p>
     * 可选值: pending, sent, failed
     * </p>
     */
    @Column(name = "notification_status", length = 20)
    private String notificationStatus;

    /**
     * 最后通知时间
     */
    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;
}

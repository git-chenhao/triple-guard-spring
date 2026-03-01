package com.chenhao.tripleguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 告警批次数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertBatchDTO {
    private Long id;
    private String name;
    private String status;
    private String severity;
    private Integer alertCount;
    private Integer uniqueCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime escalatedAt;
    private LocalDateTime resolvedAt;
    private String assignee;
    private String summary;
    private String notes;
    private String notificationStatus;
    private LocalDateTime lastNotifiedAt;
}

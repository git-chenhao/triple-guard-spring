package com.chenhao.tripleguard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警数据传输对象
 * <p>
 * 用于API层与前端之间的告警数据传输，
 * 包含字段验证规则
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDTO {

    /**
     * 告警ID
     */
    private Long id;

    /**
     * 告警指纹
     */
    private String fingerprint;

    /**
     * 告警名称
     */
    @NotBlank(message = "告警名称不能为空")
    @Size(max = 255, message = "告警名称长度不能超过255个字符")
    private String alertname;

    /**
     * 严重级别
     * <p>
     * 可选值: critical, warning, info
     * </p>
     */
    private String severity;

    /**
     * 告警状态
     * <p>
     * 可选值: firing, resolved
     * </p>
     */
    private String status;

    /**
     * 标签集合
     */
    private Map<String, String> labels;

    /**
     * 注解集合
     */
    private Map<String, String> annotations;

    /**
     * 告警来源
     */
    private String source;

    /**
     * 接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 所属批次ID
     */
    private Long batchId;

    /**
     * 告警摘要
     */
    @Size(max = 500, message = "摘要长度不能超过500个字符")
    private String summary;

    /**
     * 告警描述
     */
    private String description;

    /**
     * 批次名称（关联查询时使用）
     */
    private String batchName;

    /**
     * 批次状态（关联查询时使用）
     */
    private String batchStatus;
}

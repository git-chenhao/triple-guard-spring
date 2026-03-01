package com.chenhao.tripleguard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 告警平台配置类
 * <p>
 * 用于配置告警聚合平台的核心参数，
 * 包括聚合策略、去重规则、通知设置等
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alert")
public class AlertConfig {

    /**
     * 聚合配置
     */
    private Aggregation aggregation = new Aggregation();

    /**
     * 去重配置
     */
    private Deduplication deduplication = new Deduplication();

    /**
     * 通知配置
     */
    private Notification notification = new Notification();

    /**
     * 清理配置
     */
    private Cleanup cleanup = new Cleanup();

    /**
     * 聚合策略配置
     */
    @Data
    public static class Aggregation {

        /**
         * 是否启用聚合
         */
        private boolean enabled = true;

        /**
         * 聚合时间窗口（秒）
         * <p>
         * 在此时间窗口内收到的相似告警将被聚合到同一批次
         * </p>
         */
        private int timeWindowSeconds = 300;

        /**
         * 单批次最大告警数
         */
        private int maxBatchSize = 100;

        /**
         * 聚合依据的标签列表
         * <p>
         * 这些标签将用于判断告警是否相似
         * </p>
         */
        private String[] groupByLabels = {"alertname", "severity", "source"};
    }

    /**
     * 去重策略配置
     */
    @Data
    public static class Deduplication {

        /**
         * 是否启用去重
         */
        private boolean enabled = true;

        /**
         * 去重时间窗口（秒）
         * <p>
         * 在此时间窗口内相同指纹的告警将被视为重复
         * </p>
         */
        private int timeWindowSeconds = 3600;

        /**
         * 指纹生成依据的字段
         */
        private String[] fingerprintFields = {"alertname", "labels"};
    }

    /**
     * 通知策略配置
     */
    @Data
    public static class Notification {

        /**
         * 是否启用通知
         */
        private boolean enabled = true;

        /**
         * 通知重试次数
         */
        private int retryCount = 3;

        /**
         * 通知重试间隔（秒）
         */
        private int retryIntervalSeconds = 60;

        /**
         * 批次创建后延迟通知时间（秒）
         * <p>
         * 用于等待更多告警聚合到批次中
         * </p>
         */
        private int delaySeconds = 30;

        /**
         * 告警级别路由配置
         * <p>
         * 不同严重级别的告警可以路由到不同的通知渠道
         * </p>
         */
        private SeverityRouting severityRouting = new SeverityRouting();
    }

    /**
     * 严重级别路由配置
     */
    @Data
    public static class SeverityRouting {

        /**
         * critical级别告警的通知渠道
         */
        private String[] critical = {"email", "sms", "webhook"};

        /**
         * warning级别告警的通知渠道
         */
        private String[] warning = {"email", "webhook"};

        /**
         * info级别告警的通知渠道
         */
        private String[] info = {"webhook"};
    }

    /**
     * 数据清理配置
     */
    @Data
    public static class Cleanup {

        /**
         * 是否启用自动清理
         */
        private boolean enabled = true;

        /**
         * 已解决告警保留天数
         */
        private int resolvedRetentionDays = 30;

        /**
         * 所有告警最大保留天数
         */
        private int maxRetentionDays = 90;

        /**
         * 清理任务执行时间（Cron表达式）
         */
        private String cronExpression = "0 0 2 * * ?";
    }
}

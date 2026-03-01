package com.chenhao.tripleguard.repository;

import com.chenhao.tripleguard.entity.AlertBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警批次数据访问层
 * <p>
 * 提供告警批次实体的CRUD操作及自定义查询方法
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Repository
public interface AlertBatchRepository extends JpaRepository<AlertBatch, Long> {

    /**
     * 根据状态查找批次列表
     *
     * @param status   批次状态
     * @param pageable 分页参数
     * @return 分页批次列表
     */
    Page<AlertBatch> findByStatus(String status, Pageable pageable);

    /**
     * 根据状态查找批次列表
     *
     * @param status 批次状态
     * @return 批次列表
     */
    List<AlertBatch> findByStatus(String status);

    /**
     * 根据状态和严重级别查找批次
     *
     * @param status   批次状态
     * @param severity 严重级别
     * @param pageable 分页参数
     * @return 分页批次列表
     */
    Page<AlertBatch> findByStatusAndSeverity(String status, String severity, Pageable pageable);

    /**
     * 查找指定时间之前创建且状态为pending的批次
     *
     * @param beforeTime 时间阈值
     * @return 批次列表
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'pending' AND b.createdAt < :beforeTime ORDER BY b.createdAt ASC")
    List<AlertBatch> findPendingBatchesCreatedBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查找指定时间范围内创建的批次
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 分页批次列表
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.createdAt BETWEEN :startTime AND :endTime ORDER BY b.createdAt DESC")
    Page<AlertBatch> findBatchesCreatedBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 统计指定状态的批次数量
     *
     * @param status 批次状态
     * @return 批次数量
     */
    long countByStatus(String status);

    /**
     * 统计指定严重级别的批次数量
     *
     * @param severity 严重级别
     * @return 批次数量
     */
    long countBySeverity(String severity);

    /**
     * 查找通知状态为pending的批次
     *
     * @return 批次列表
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.notificationStatus = 'pending' ORDER BY b.createdAt ASC")
    List<AlertBatch> findPendingNotificationBatches();

    /**
     * 根据通知状态查找批次
     *
     * @param notificationStatus 通知状态
     * @return 批次列表
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.notificationStatus = :notificationStatus ORDER BY b.createdAt ASC")
    List<AlertBatch> findByNotificationStatus(@Param("notificationStatus") String notificationStatus);

    /**
     * 根据严重级别查找批次
     *
     * @param severity 严重级别
     * @param pageable 分页参数
     * @return 分页批次列表
     */
    Page<AlertBatch> findBySeverity(String severity, Pageable pageable);

    /**
     * 查找指定处理人的批次
     *
     * @param assignee 处理人
     * @param pageable 分页参数
     * @return 分页批次列表
     */
    Page<AlertBatch> findByAssignee(String assignee, Pageable pageable);
}

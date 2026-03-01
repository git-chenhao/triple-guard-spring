package com.chenhao.tripleguard.repository;

import com.chenhao.tripleguard.entity.Alert;
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
 * 告警数据访问层
 * <p>
 * 提供告警实体的CRUD操作及自定义查询方法
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 根据指纹查找告警
     *
     * @param fingerprint 告警指纹
     * @return 告警对象（可能为空）
     */
    Optional<Alert> findByFingerprint(String fingerprint);

    /**
     * 根据状态查找告警列表
     *
     * @param status   告警状态
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    Page<Alert> findByStatus(String status, Pageable pageable);

    /**
     * 根据批次ID查找告警列表
     *
     * @param batchId 批次ID
     * @return 告警列表
     */
    List<Alert> findByBatchId(Long batchId);

    /**
     * 根据批次ID查找告警列表（分页）
     *
     * @param batchId  批次ID
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    Page<Alert> findByBatchId(Long batchId, Pageable pageable);

    /**
     * 根据来源查找告警
     *
     * @param source   告警来源
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    Page<Alert> findBySource(String source, Pageable pageable);

    /**
     * 根据严重级别查找告警
     *
     * @param severity 严重级别
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    Page<Alert> findBySeverity(String severity, Pageable pageable);

    /**
     * 查找未分配批次的告警
     *
     * @return 未分配批次的告警列表
     */
    @Query("SELECT a FROM Alert a WHERE a.batchId IS NULL ORDER BY a.receivedAt DESC")
    List<Alert> findUnbatchedAlerts();

    /**
     * 查找指定时间范围内接收的告警
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 分页告警列表
     */
    @Query("SELECT a FROM Alert a WHERE a.receivedAt BETWEEN :startTime AND :endTime ORDER BY a.receivedAt DESC")
    Page<Alert> findByReceivedAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 统计指定状态的告警数量
     *
     * @param status 告警状态
     * @return 告警数量
     */
    long countByStatus(String status);

    /**
     * 统计指定批次的告警数量
     *
     * @param batchId 批次ID
     * @return 告警数量
     */
    long countByBatchId(Long batchId);

    /**
     * 检查指定指纹的告警是否存在
     *
     * @param fingerprint 告警指纹
     * @return 是否存在
     */
    boolean existsByFingerprint(String fingerprint);

    /**
     * 查找指定时间范围内且未分配批次的告警
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 告警列表
     */
    @Query("SELECT a FROM Alert a WHERE a.batchId IS NULL AND a.receivedAt BETWEEN :startTime AND :endTime ORDER BY a.receivedAt DESC")
    List<Alert> findUnbatchedAlertsBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据状态和严重级别查找告警
     *
     * @param status   告警状态
     * @param severity 严重级别
     * @param pageable 分页参数
     * @return 分页告警列表
     */
    Page<Alert> findByStatusAndSeverity(String status, String severity, Pageable pageable);

    /**
     * 批量更新告警的批次ID
     *
     * @param ids     告警ID列表
     * @param batchId 批次ID
     * @return 更新的记录数
     */
    @Query("UPDATE Alert a SET a.batchId = :batchId WHERE a.id IN :ids")
    int updateBatchIdByIds(@Param("ids") List<Long> ids, @Param("batchId") Long batchId);

    /**
     * 查找重复的告警（根据指纹分组）
     *
     * @param fingerprint 告警指纹
     * @param pageable    分页参数
     * @return 分页告警列表
     */
    Page<Alert> findByFingerprintOrderByReceivedAtDesc(String fingerprint, Pageable pageable);
}

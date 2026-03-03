package com.alerthub.repository;

import com.alerthub.model.Alert;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.AlertStatus;
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
 * 告警数据访问接口
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 根据指纹查找告警
     */
    Optional<Alert> findByFingerprint(String fingerprint);

    /**
     * 检查指纹是否存在
     */
    boolean existsByFingerprint(String fingerprint);

    /**
     * 根据状态查找告警
     */
    List<Alert> findByStatus(AlertStatus status);

    /**
     * 根据状态分页查询
     */
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * 根据来源查找告警
     */
    List<Alert> findBySource(String source);

    /**
     * 根据来源和状态查找告警
     */
    List<Alert> findBySourceAndStatus(String source, AlertStatus status);

    /**
     * 根据批次 ID 查找告警
     */
    List<Alert> findByBatchId(Long batchId);

    /**
     * 查找指定时间范围内的告警
     */
    @Query("SELECT a FROM Alert a WHERE a.receivedAt BETWEEN :start AND :end")
    List<Alert> findByReceivedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 查找未分配批次的告警
     */
    @Query("SELECT a FROM Alert a WHERE a.batchId IS NULL AND a.status = :status")
    List<Alert> findUnbatchedAlerts(@Param("status") AlertStatus status);

    /**
     * 统计指定状态的告警数量
     */
    long countByStatus(AlertStatus status);

    /**
     * 统计指定来源的告警数量
     */
    long countBySource(String source);

    /**
     * 统计指定时间范围内的告警数量
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.receivedAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    /**
     * 按来源和严重级别统计
     */
    @Query("SELECT a.source, a.severity, COUNT(a) FROM Alert a " +
           "WHERE a.receivedAt >= :since GROUP BY a.source, a.severity")
    List<Object[]> countBySourceAndSeveritySince(@Param("since") LocalDateTime since);

    /**
     * 删除指定时间之前的告警
     */
    void deleteByReceivedAtBefore(LocalDateTime before);

    /**
     * 查找需要重试的告警
     */
    @Query("SELECT a FROM Alert a WHERE a.status = :status AND a.retryCount < :maxRetries")
    List<Alert> findRetryableAlerts(@Param("status") AlertStatus status, @Param("maxRetries") int maxRetries);

    /**
     * 按严重级别查询
     */
    List<Alert> findBySeverity(AlertSeverity severity);

    /**
     * 按严重级别和状态查询
     */
    List<Alert> findBySeverityAndStatus(AlertSeverity severity, AlertStatus status);

    /**
     * 多条件分页查询
     */
    @Query("SELECT a FROM Alert a WHERE " +
           "(:source IS NULL OR a.source = :source) AND " +
           "(:severity IS NULL OR a.severity = :severity) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:startTime IS NULL OR a.receivedAt >= :startTime) AND " +
           "(:endTime IS NULL OR a.receivedAt <= :endTime)")
    Page<Alert> searchAlerts(
            @Param("source") String source,
            @Param("severity") AlertSeverity severity,
            @Param("status") AlertStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}

package com.alerthub.repository;

import com.alerthub.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
    Page<Alert> findByStatus(String status, Pageable pageable);

    /**
     * 根据批次 ID 查找告警
     */
    List<Alert> findByBatchId(Long batchId);

    /**
     * 查找指定时间范围内的待处理告警
     */
    @Query("SELECT a FROM Alert a WHERE a.status = 'pending' AND a.createdAt >= :startTime")
    List<Alert> findPendingAlertsAfter(@Param("startTime") LocalDateTime startTime);

    /**
     * 查找指定时间范围内的待处理告警（带数量限制，防止内存溢出）
     */
    @Query("SELECT a FROM Alert a WHERE a.status = 'pending' AND a.createdAt >= :startTime ORDER BY a.createdAt ASC LIMIT :limit")
    List<Alert> findPendingAlertsAfterWithLimit(@Param("startTime") LocalDateTime startTime, @Param("limit") int limit);

    /**
     * 统计指定状态的告警数量
     */
    long countByStatus(String status);

    /**
     * 统计指定时间范围内的告警数量
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.createdAt BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的告警
     */
    List<Alert> findTop10ByOrderByCreatedAtDesc();

    /**
     * 根据来源统计告警数量
     */
    @Query("SELECT a.source, COUNT(a) FROM Alert a GROUP BY a.source")
    List<Object[]> countBySource();

    /**
     * 根据级别统计告警数量
     */
    @Query("SELECT a.severity, COUNT(a) FROM Alert a GROUP BY a.severity")
    List<Object[]> countBySeverity();

    /**
     * 批量更新告警状态和批次ID
     */
    @Modifying
    @Query("UPDATE Alert a SET a.status = :status, a.batchId = :batchId WHERE a.id IN :ids")
    int batchUpdateStatusAndBatchId(@Param("ids") List<Long> ids, @Param("status") String status, @Param("batchId") Long batchId);
}

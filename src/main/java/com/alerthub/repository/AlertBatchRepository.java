package com.alerthub.repository;

import com.alerthub.model.AlertBatch;
import com.alerthub.model.AlertSeverity;
import com.alerthub.model.BatchStatus;
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
 * 告警批次数据访问接口
 */
@Repository
public interface AlertBatchRepository extends JpaRepository<AlertBatch, Long> {

    /**
     * 根据批次标识查找
     */
    Optional<AlertBatch> findByBatchKey(String batchKey);

    /**
     * 检查批次标识是否存在
     */
    boolean existsByBatchKey(String batchKey);

    /**
     * 根据状态查找批次
     */
    List<AlertBatch> findByStatus(BatchStatus status);

    /**
     * 根据状态分页查询
     */
    Page<AlertBatch> findByStatus(BatchStatus status, Pageable pageable);

    /**
     * 根据来源查找批次
     */
    List<AlertBatch> findBySource(String source);

    /**
     * 查找需要处理的批次（时间窗口已结束）
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = :status AND b.windowEnd <= :now")
    List<AlertBatch> findProcessableBatches(@Param("status") BatchStatus status, @Param("now") LocalDateTime now);

    /**
     * 查找活跃批次（在时间窗口内）
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'AGGREGATING' AND b.windowStart <= :now AND b.windowEnd > :now")
    List<AlertBatch> findActiveBatches(@Param("now") LocalDateTime now);

    /**
     * 根据来源、严重级别和时间窗口查找活跃批次
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.source = :source AND b.severity = :severity " +
           "AND b.status = 'AGGREGATING' AND b.windowStart <= :now AND b.windowEnd > :now")
    Optional<AlertBatch> findActiveBatch(
            @Param("source") String source,
            @Param("severity") AlertSeverity severity,
            @Param("now") LocalDateTime now);

    /**
     * 增加告警计数
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.alertCount = b.alertCount + 1, b.updatedAt = :now WHERE b.id = :batchId")
    void incrementAlertCount(@Param("batchId") Long batchId, @Param("now") LocalDateTime now);

    /**
     * 更新批次状态
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.status = :status, b.updatedAt = :now WHERE b.id = :batchId")
    void updateStatus(@Param("batchId") Long batchId, @Param("status") BatchStatus status, @Param("now") LocalDateTime now);

    /**
     * 统计指定状态的批次数量
     */
    long countByStatus(BatchStatus status);

    /**
     * 统计指定时间范围内的批次数量
     */
    @Query("SELECT COUNT(b) FROM AlertBatch b WHERE b.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    /**
     * 删除指定时间之前的批次
     */
    void deleteByCreatedAtBefore(LocalDateTime before);

    /**
     * 查找未发送通知的已完成批次
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'PENDING_NOTIFICATION' ORDER BY b.createdAt ASC")
    List<AlertBatch> findPendingNotificationBatches();

    /**
     * 查找需要分析的批次
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'PENDING_ANALYSIS' ORDER BY b.createdAt ASC")
    List<AlertBatch> findPendingAnalysisBatches();

    /**
     * 多条件分页查询
     */
    @Query("SELECT b FROM AlertBatch b WHERE " +
           "(:source IS NULL OR b.source = :source) AND " +
           "(:severity IS NULL OR b.severity = :severity) AND " +
           "(:status IS NULL OR b.status = :status) AND " +
           "(:startTime IS NULL OR b.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR b.createdAt <= :endTime)")
    Page<AlertBatch> searchBatches(
            @Param("source") String source,
            @Param("severity") AlertSeverity severity,
            @Param("status") BatchStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}

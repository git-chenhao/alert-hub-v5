package com.alerthub.repository;

import com.alerthub.entity.AlertBatch;
import org.springframework.data.jpa.repository.JpaRepository;
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
     * 根据批次号查找
     */
    Optional<AlertBatch> findByBatchNo(String batchNo);

    /**
     * 根据状态查找批次
     */
    List<AlertBatch> findByStatus(String status);

    /**
     * 查找指定时间范围内开放的批次
     */
    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'open' AND b.windowStart <= :endTime")
    List<AlertBatch> findOpenBatchesBefore(@Param("endTime") LocalDateTime endTime);

    /**
     * 查找最近的批次
     */
    List<AlertBatch> findTop10ByOrderByCreatedAtDesc();

    /**
     * 统计指定状态的批次数量
     */
    long countByStatus(String status);

    /**
     * 统计指定时间范围内的批次数量
     */
    @Query("SELECT COUNT(b) FROM AlertBatch b WHERE b.createdAt BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}

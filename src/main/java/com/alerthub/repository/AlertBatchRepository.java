package com.alerthub.repository;

import com.alerthub.entity.AlertBatch;
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
 * 告警批次 Repository
 */
@Repository
public interface AlertBatchRepository extends JpaRepository<AlertBatch, Long> {

    Optional<AlertBatch> findByBatchNo(String batchNo);

    List<AlertBatch> findByStatus(String status);

    Page<AlertBatch> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT b FROM AlertBatch b WHERE b.createdAt BETWEEN :start AND :end ORDER BY b.createdAt DESC")
    List<AlertBatch> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT b FROM AlertBatch b WHERE b.status = 'open' AND b.windowEnd <= :now ORDER BY b.windowEnd ASC")
    List<AlertBatch> findReadyToProcessBatches(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM AlertBatch b WHERE b.status = :status")
    long countByStatus(@Param("status") String status);
}

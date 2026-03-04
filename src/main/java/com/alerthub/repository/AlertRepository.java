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
 * 告警 Repository
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findByAlertId(String alertId);

    Optional<Alert> findByFingerprint(String fingerprint);

    List<Alert> findByStatus(String status);

    List<Alert> findByBatchId(Long batchId);

    List<Alert> findByBatchIdIsNull();

    Page<Alert> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<Alert> findByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Alert a WHERE a.status = :status AND a.createdAt >= :since ORDER BY a.createdAt ASC")
    List<Alert> findPendingAlertsSince(@Param("status") String status, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.status = :status")
    long countByStatus(@Param("status") String status);

    @Query("SELECT a.severity, COUNT(a) FROM Alert a GROUP BY a.severity")
    List<Object[]> countBySeverity();

    @Query("SELECT a.source, COUNT(a) FROM Alert a GROUP BY a.source ORDER BY COUNT(a) DESC")
    List<Object[]> countBySource();

    void deleteByCreatedAtBefore(LocalDateTime cutoff);

    /**
     * 批量更新告警状态
     */
    @Modifying
    @Query("UPDATE Alert a SET a.status = :status WHERE a.id IN :ids")
    int updateStatusByIds(@Param("status") String status, @Param("ids") List<Long> ids);

    /**
     * 批量更新告警状态和根因分析结果
     */
    @Modifying
    @Query("UPDATE Alert a SET a.status = :status, a.rootCauseAnalysis = :rootCause WHERE a.id IN :ids")
    int updateStatusAndRootCauseByIds(@Param("status") String status, @Param("rootCause") String rootCause, @Param("ids") List<Long> ids);
}

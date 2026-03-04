package com.alerthub.repository;

import com.alerthub.entity.DeduplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 去重记录 Repository
 */
@Repository
public interface DeduplicationRecordRepository extends JpaRepository<DeduplicationRecord, Long> {

    Optional<DeduplicationRecord> findByFingerprint(String fingerprint);

    boolean existsByFingerprint(String fingerprint);

    @Modifying
    @Query("UPDATE DeduplicationRecord d SET d.lastSeenAt = :now, d.occurrenceCount = d.occurrenceCount + 1 WHERE d.fingerprint = :fingerprint")
    int updateLastSeen(@Param("fingerprint") String fingerprint, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM DeduplicationRecord d WHERE d.expiresAt < :now")
    int deleteExpiredRecords(@Param("now") LocalDateTime now);
}

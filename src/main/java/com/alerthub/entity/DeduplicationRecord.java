package com.alerthub.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 去重记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "deduplication_records", indexes = {
    @Index(name = "idx_dedup_fingerprint", columnList = "fingerprint", unique = true),
    @Index(name = "idx_dedup_expires_at", columnList = "expiresAt")
})
public class DeduplicationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 指纹 */
    @Column(length = 64, nullable = false, unique = true)
    private String fingerprint;

    /** 首次出现时间 */
    @CreationTimestamp
    private LocalDateTime firstSeenAt;

    /** 最后出现时间 */
    private LocalDateTime lastSeenAt;

    /** 出现次数 */
    @Builder.Default
    private Integer occurrenceCount = 1;

    /** 过期时间 */
    private LocalDateTime expiresAt;
}

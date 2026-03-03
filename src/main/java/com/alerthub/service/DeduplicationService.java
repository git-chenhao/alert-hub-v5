package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.model.Alert;
import com.alerthub.repository.AlertRepository;
import com.alerthub.util.FingerprintUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警去重服务
 * 使用内存缓存 + 数据库双重检查
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final AlertRepository alertRepository;
    private final AlertHubProperties properties;

    // 内存缓存：fingerprint -> 最后出现时间
    private final Map<String, LocalDateTime> fingerprintCache = new ConcurrentHashMap<>();

    /**
     * 检查告警是否为重复告警
     *
     * @param source   告警来源
     * @param severity 严重级别
     * @param title    告警标题
     * @param labels   标签
     * @return 如果是重复告警返回 true
     */
    public boolean isDuplicate(String source, String severity, String title, Map<String, String> labels) {
        String fingerprint = FingerprintUtil.generate(source, severity, title, labels);
        return isDuplicateByFingerprint(fingerprint);
    }

    /**
     * 根据指纹检查是否为重复告警
     */
    public boolean isDuplicateByFingerprint(String fingerprint) {
        // 1. 检查内存缓存
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cacheExpiry = now.minusMinutes(properties.getDeduplication().getCacheTtlMinutes());

        LocalDateTime lastSeen = fingerprintCache.get(fingerprint);
        if (lastSeen != null && lastSeen.isAfter(cacheExpiry)) {
            log.debug("Duplicate alert found in cache: {}", fingerprint);
            return true;
        }

        // 2. 检查数据库
        boolean existsInDb = alertRepository.existsByFingerprint(fingerprint);
        if (existsInDb) {
            // 更新缓存
            fingerprintCache.put(fingerprint, now);
            log.debug("Duplicate alert found in database: {}", fingerprint);
            return true;
        }

        return false;
    }

    /**
     * 记录新告警指纹到缓存
     */
    public void recordFingerprint(String fingerprint) {
        fingerprintCache.put(fingerprint, LocalDateTime.now());
        log.debug("Recorded new fingerprint: {}", fingerprint);
    }

    /**
     * 生成告警指纹
     */
    public String generateFingerprint(String source, String severity, String title, Map<String, String> labels) {
        return FingerprintUtil.generate(source, severity, title, labels);
    }

    /**
     * 清理过期的缓存条目
     */
    public void cleanupExpiredCache() {
        LocalDateTime cacheExpiry = LocalDateTime.now()
                .minusMinutes(properties.getDeduplication().getCacheTtlMinutes());

        fingerprintCache.entrySet().removeIf(entry -> entry.getValue().isBefore(cacheExpiry));

        log.debug("Cleaned up expired fingerprint cache, remaining entries: {}", fingerprintCache.size());
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return fingerprintCache.size();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        fingerprintCache.clear();
        log.info("Fingerprint cache cleared");
    }
}

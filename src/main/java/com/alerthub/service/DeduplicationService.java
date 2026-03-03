package com.alerthub.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 告警去重服务
 */
@Slf4j
@Service
public class DeduplicationService {

    private final Cache deduplicationCache;

    @Value("${alerthub.deduplication.enabled:true}")
    private boolean enabled;

    public DeduplicationService(CacheManager cacheManager) {
        this.deduplicationCache = cacheManager.getCache("deduplication");
    }

    /**
     * 检查是否重复
     */
    public boolean isDuplicate(String fingerprint) {
        if (!enabled) {
            return false;
        }

        if (deduplicationCache == null) {
            log.warn("去重缓存未初始化");
            return false;
        }

        Boolean exists = deduplicationCache.get(fingerprint, Boolean.class);
        if (exists != null && exists) {
            log.debug("告警重复: fingerprint={}", fingerprint);
            return true;
        }

        // 标记为已存在
        deduplicationCache.put(fingerprint, Boolean.TRUE);
        return false;
    }

    /**
     * 清除指纹缓存
     */
    public void clearFingerprint(String fingerprint) {
        if (deduplicationCache != null) {
            deduplicationCache.evict(fingerprint);
            log.debug("清除指纹缓存: {}", fingerprint);
        }
    }

    /**
     * 清除所有缓存
     */
    public void clearAll() {
        if (deduplicationCache != null) {
            deduplicationCache.clear();
            log.info("清除所有去重缓存");
        }
    }
}

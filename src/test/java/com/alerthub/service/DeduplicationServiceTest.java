package com.alerthub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DeduplicationService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache deduplicationCache;

    private DeduplicationService service;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("deduplication")).thenReturn(deduplicationCache);
        service = new DeduplicationService(cacheManager);
        // 无 Spring 上下文时 boolean 默认 false，需手动设为 true 模拟生产行为
        ReflectionTestUtils.setField(service, "enabled", true);
    }

    // ========== isDuplicate 测试 ==========

    /**
     * 场景 1.1: 首次检查指纹，返回 false 并写入缓存
     */
    @Test
    void isDuplicate_firstTime_returnsFalse() {
        // Given
        when(deduplicationCache.get("fp1", Boolean.class)).thenReturn(null);

        // When
        boolean result = service.isDuplicate("fp1");

        // Then
        assertFalse(result);
        verify(deduplicationCache).put("fp1", Boolean.TRUE);
    }

    /**
     * 场景 1.2: 重复检查指纹，返回 true
     */
    @Test
    void isDuplicate_alreadyExists_returnsTrue() {
        // Given
        when(deduplicationCache.get("fp1", Boolean.class)).thenReturn(Boolean.TRUE);

        // When
        boolean result = service.isDuplicate("fp1");

        // Then
        assertTrue(result);
        verify(deduplicationCache, never()).put(anyString(), any());
    }

    /**
     * 场景 1.3: enabled=false 时直接返回 false，不访问缓存
     */
    @Test
    void isDuplicate_disabled_returnsFalse() {
        // Given
        ReflectionTestUtils.setField(service, "enabled", false);

        // When
        boolean result = service.isDuplicate("fp1");

        // Then
        assertFalse(result);
        verifyNoInteractions(deduplicationCache);
    }

    /**
     * 场景 1.4: 缓存未初始化（CacheManager 返回 null），安全降级返回 false
     */
    @Test
    void isDuplicate_cacheNull_returnsFalse() {
        // Given
        when(cacheManager.getCache("deduplication")).thenReturn(null);
        DeduplicationService svcWithNullCache = new DeduplicationService(cacheManager);
        ReflectionTestUtils.setField(svcWithNullCache, "enabled", true);

        // When
        boolean result = svcWithNullCache.isDuplicate("fp1");

        // Then
        assertFalse(result);
    }

    // ========== clearFingerprint 测试 ==========

    /**
     * 场景 2.1: 清除已存在指纹
     */
    @Test
    void clearFingerprint_existing_removesFromCache() {
        // When
        service.clearFingerprint("fp1");

        // Then
        verify(deduplicationCache).evict("fp1");
    }

    /**
     * 场景 2.2: 缓存未初始化时清除指纹，不抛异常
     */
    @Test
    void clearFingerprint_cacheNull_noException() {
        // Given
        when(cacheManager.getCache("deduplication")).thenReturn(null);
        DeduplicationService svcWithNullCache = new DeduplicationService(cacheManager);

        // When & Then
        assertDoesNotThrow(() -> svcWithNullCache.clearFingerprint("fp1"));
    }

    // ========== clearAll 测试 ==========

    /**
     * 场景 3.1: 清除所有缓存
     */
    @Test
    void clearAll_clearsCache() {
        // When
        service.clearAll();

        // Then
        verify(deduplicationCache).clear();
    }

    /**
     * 场景 3.2: 缓存未初始化时清除所有，不抛异常
     */
    @Test
    void clearAll_cacheNull_noException() {
        // Given
        when(cacheManager.getCache("deduplication")).thenReturn(null);
        DeduplicationService svcWithNullCache = new DeduplicationService(cacheManager);

        // When & Then
        assertDoesNotThrow(() -> svcWithNullCache.clearAll());
    }
}

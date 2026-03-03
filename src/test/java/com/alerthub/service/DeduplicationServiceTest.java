package com.alerthub.service;

import com.alerthub.config.AlertHubProperties;
import com.alerthub.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * DeduplicationService 测试类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeduplicationServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertHubProperties properties;

    @Mock
    private AlertHubProperties.DeduplicationConfig deduplicationConfig;

    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        when(properties.getDeduplication()).thenReturn(deduplicationConfig);
        when(deduplicationConfig.getCacheTtlMinutes()).thenReturn(60);

        deduplicationService = new DeduplicationService(alertRepository, properties);
    }

    @Test
    void testIsDuplicate_FirstOccurrence() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        when(alertRepository.existsByFingerprint(anyString())).thenReturn(false);

        boolean isDuplicate = deduplicationService.isDuplicate("prometheus", "HIGH", "CPU Alert", labels);

        assertFalse(isDuplicate);
        verify(alertRepository, times(1)).existsByFingerprint(anyString());
    }

    @Test
    void testIsDuplicate_FoundInDatabase() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        when(alertRepository.existsByFingerprint(anyString())).thenReturn(true);

        boolean isDuplicate = deduplicationService.isDuplicate("prometheus", "HIGH", "CPU Alert", labels);

        assertTrue(isDuplicate);
        verify(alertRepository, times(1)).existsByFingerprint(anyString());
    }

    @Test
    void testIsDuplicate_FoundInCache() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        // 第一次查询，记录到缓存
        when(alertRepository.existsByFingerprint(anyString())).thenReturn(false);
        boolean firstResult = deduplicationService.isDuplicate("prometheus", "HIGH", "CPU Alert", labels);
        assertFalse(firstResult);

        // 记录指纹
        String fingerprint = deduplicationService.generateFingerprint("prometheus", "HIGH", "CPU Alert", labels);
        deduplicationService.recordFingerprint(fingerprint);

        // 第二次查询，应该从缓存中找到
        boolean secondResult = deduplicationService.isDuplicate("prometheus", "HIGH", "CPU Alert", labels);
        assertTrue(secondResult);
    }

    @Test
    void testRecordFingerprint() {
        String fingerprint = "test-fingerprint";

        deduplicationService.recordFingerprint(fingerprint);

        // 验证缓存中有记录
        assertTrue(deduplicationService.isDuplicateByFingerprint(fingerprint));
    }

    @Test
    void testClearCache() {
        String fingerprint = "test-fingerprint";
        deduplicationService.recordFingerprint(fingerprint);

        deduplicationService.clearCache();

        assertEquals(0, deduplicationService.getCacheSize());
    }

    @Test
    void testGenerateFingerprint() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        String fingerprint = deduplicationService.generateFingerprint("prometheus", "HIGH", "CPU Alert", labels);

        assertNotNull(fingerprint);
        assertEquals(64, fingerprint.length());
    }
}

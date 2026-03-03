package com.alerthub.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FingerprintUtil 测试类
 */
class FingerprintUtilTest {

    @Test
    void testGenerateFingerprint() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");
        labels.put("service", "api");

        String fingerprint1 = FingerprintUtil.generate("prometheus", "HIGH", "CPU Alert", labels);
        String fingerprint2 = FingerprintUtil.generate("prometheus", "HIGH", "CPU Alert", labels);

        // 相同输入应生成相同指纹
        assertEquals(fingerprint1, fingerprint2);

        // 指纹长度应为 64（SHA-256）
        assertEquals(64, fingerprint1.length());
    }

    @Test
    void testGenerateFingerprintWithDifferentLabels() {
        Map<String, String> labels1 = new HashMap<>();
        labels1.put("env", "prod");
        labels1.put("service", "api");

        Map<String, String> labels2 = new HashMap<>();
        labels2.put("service", "api");
        labels2.put("env", "prod");

        // 标签顺序不同，但内容相同，应生成相同指纹
        String fingerprint1 = FingerprintUtil.generate("prometheus", "HIGH", "CPU Alert", labels1);
        String fingerprint2 = FingerprintUtil.generate("prometheus", "HIGH", "CPU Alert", labels2);

        assertEquals(fingerprint1, fingerprint2);
    }

    @Test
    void testGenerateFingerprintWithDifferentInputs() {
        Map<String, String> labels = new HashMap<>();
        labels.put("env", "prod");

        String fingerprint1 = FingerprintUtil.generate("prometheus", "HIGH", "CPU Alert", labels);
        String fingerprint2 = FingerprintUtil.generate("prometheus", "LOW", "CPU Alert", labels);

        // 不同输入应生成不同指纹
        assertNotEquals(fingerprint1, fingerprint2);
    }

    @Test
    void testGenerateSimpleFingerprint() {
        String fingerprint1 = FingerprintUtil.generateSimple("prometheus", "HIGH", "CPU Alert");
        String fingerprint2 = FingerprintUtil.generateSimple("prometheus", "HIGH", "CPU Alert");

        assertEquals(fingerprint1, fingerprint2);
        assertEquals(64, fingerprint1.length());
    }

    @Test
    void testGenerateFingerprintWithNullValues() {
        String fingerprint = FingerprintUtil.generate(null, null, null, null);
        assertNotNull(fingerprint);
        assertEquals(64, fingerprint.length());
    }

    @Test
    void testGenerateFingerprintWithEmptyValues() {
        String fingerprint = FingerprintUtil.generate("", "", "", new HashMap<>());
        assertNotNull(fingerprint);
        assertEquals(64, fingerprint.length());
    }
}

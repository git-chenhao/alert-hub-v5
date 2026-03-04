package com.alerthub.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 指纹计算工具类
 */
@Slf4j
public class FingerprintUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 计算 MD5 指纹
     */
    public static String md5(String input) {
        return calculateHash(input, "MD5");
    }

    /**
     * 计算 SHA256 指纹
     */
    public static String sha256(String input) {
        return calculateHash(input, "SHA-256");
    }

    /**
     * 根据算法计算哈希
     */
    public static String calculateHash(String input, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("不支持的哈希算法: {}", algorithm, e);
            throw new RuntimeException("不支持的哈希算法: " + algorithm, e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 计算告警指纹
     */
    public static String calculateAlertFingerprint(String source, String alertName, String severity, Map<String, String> labels) {
        String labelsJson = serializeMap(labels);
        String raw = String.format("%s|%s|%s|%s",
            nullToEmpty(source),
            nullToEmpty(alertName),
            nullToEmpty(severity),
            nullToEmpty(labelsJson)
        );
        return md5(raw);
    }

    /**
     * 使用指定算法计算告警指纹
     */
    public static String calculateAlertFingerprint(String source, String alertName, String severity,
                                                    Map<String, String> labels, String algorithm) {
        String labelsJson = serializeMap(labels);
        String raw = String.format("%s|%s|%s|%s",
            nullToEmpty(source),
            nullToEmpty(alertName),
            nullToEmpty(severity),
            nullToEmpty(labelsJson)
        );
        return calculateHash(raw, algorithm);
    }

    /**
     * 序列化 Map
     */
    private static String serializeMap(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            log.warn("序列化 labels 失败", ex);
            return map.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.joining(","));
        }
    }

    private static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
}

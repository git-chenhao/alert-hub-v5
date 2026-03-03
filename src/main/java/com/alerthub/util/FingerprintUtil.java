package com.alerthub.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * 指纹工具类 - 用于告警去重
 */
public final class FingerprintUtil {

    private static final Logger log = LoggerFactory.getLogger(FingerprintUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private FingerprintUtil() {
        // 私有构造函数，防止实例化
    }

    /**
     * 生成告警指纹
     * 基于 source + severity + title + labels 生成 SHA-256 哈希
     *
     * @param source   告警来源
     * @param severity 严重级别
     * @param title    告警标题
     * @param labels   标签
     * @return SHA-256 指纹字符串
     */
    public static String generate(String source, String severity, String title, Map<String, String> labels) {
        // 构建指纹字符串
        StringBuilder sb = new StringBuilder();
        sb.append("source=").append(nullToEmpty(source)).append(";");
        sb.append("severity=").append(nullToEmpty(severity)).append(";");
        sb.append("title=").append(nullToEmpty(title)).append(";");

        // 对标签进行排序以保证相同的标签生成相同的指纹
        if (labels != null && !labels.isEmpty()) {
            Map<String, String> sortedLabels = new TreeMap<>(labels);
            try {
                sb.append("labels=").append(OBJECT_MAPPER.writeValueAsString(sortedLabels));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize labels for fingerprint: {}", e.getMessage());
                sb.append("labels=").append(sortedLabels.toString());
            }
        }

        return sha256(sb.toString());
    }

    /**
     * 生成简化的告警指纹（不含标签）
     */
    public static String generateSimple(String source, String severity, String title) {
        String content = String.format("source=%s;severity=%s;title=%s",
                nullToEmpty(source),
                nullToEmpty(severity),
                nullToEmpty(title));
        return sha256(content);
    }

    /**
     * SHA-256 哈希
     */
    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * null 转空字符串
     */
    private static String nullToEmpty(String str) {
        return str == null ? "" : str.trim();
    }
}

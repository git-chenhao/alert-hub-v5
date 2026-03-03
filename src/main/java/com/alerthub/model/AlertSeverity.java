package com.alerthub.model;

/**
 * 告警严重级别枚举
 */
public enum AlertSeverity {
    /**
     * 严重 - 需要立即处理
     */
    CRITICAL(4),

    /**
     * 高 - 需要尽快处理
     */
    HIGH(3),

    /**
     * 中 - 需要在工作时间内处理
     */
    MEDIUM(2),

    /**
     * 低 - 可以延后处理
     */
    LOW(1),

    /**
     * 信息 - 仅供参考
     */
    INFO(0);

    private final int level;

    AlertSeverity(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * 判断当前级别是否 >= 指定级别
     */
    public boolean isAtLeast(AlertSeverity severity) {
        return this.level >= severity.level;
    }

    /**
     * 从字符串解析严重级别
     */
    public static AlertSeverity fromString(String value) {
        if (value == null || value.isEmpty()) {
            return INFO;
        }
        try {
            return AlertSeverity.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }
}

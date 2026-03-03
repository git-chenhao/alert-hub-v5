package com.alerthub.model;

/**
 * 批次状态枚举
 */
public enum BatchStatus {
    /**
     * 聚合中（时间窗口内）
     */
    AGGREGATING,

    /**
     * 等待分析
     */
    PENDING_ANALYSIS,

    /**
     * 分析中
     */
    ANALYZING,

    /**
     * 等待通知
     */
    PENDING_NOTIFICATION,

    /**
     * 通知中
     */
    NOTIFYING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 处理失败
     */
    FAILED
}

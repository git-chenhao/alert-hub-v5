package com.alerthub.model;

/**
 * 告警状态枚举
 */
public enum AlertStatus {
    /**
     * 已接收，待处理
     */
    RECEIVED,

    /**
     * 已去重（重复告警）
     */
    DUPLICATED,

    /**
     * 已聚合到批次
     */
    AGGREGATED,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 已完成
     */
    COMPLETED,

    /**
     * 处理失败
     */
    FAILED
}

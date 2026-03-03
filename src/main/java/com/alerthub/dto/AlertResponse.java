package com.alerthub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public static <T> AlertResponse<T> success(T data) {
        return AlertResponse.<T>builder()
            .code(200)
            .message("success")
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static <T> AlertResponse<T> success(String message, T data) {
        return AlertResponse.<T>builder()
            .code(200)
            .message(message)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static <T> AlertResponse<T> error(Integer code, String message) {
        return AlertResponse.<T>builder()
            .code(code)
            .message(message)
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static <T> AlertResponse<T> error(String message) {
        return error(500, message);
    }
}

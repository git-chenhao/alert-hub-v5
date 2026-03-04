package com.alerthub.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * API 响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> AlertResponse<T> success(T data) {
        return AlertResponse.<T>builder()
            .code(200)
            .message("success")
            .data(data)
            .build();
    }

    public static <T> AlertResponse<T> success(String message, T data) {
        return AlertResponse.<T>builder()
            .code(200)
            .message(message)
            .data(data)
            .build();
    }

    public static <T> AlertResponse<T> error(int code, String message) {
        return AlertResponse.<T>builder()
            .code(code)
            .message(message)
            .build();
    }

    public static <T> AlertResponse<T> error(String message) {
        return error(500, message);
    }
}

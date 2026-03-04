package com.alerthub.config;

import com.alerthub.dto.AlertResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AlertResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("参数验证失败");
        log.warn("参数验证失败: {}", message);
        return ResponseEntity.badRequest()
            .body(AlertResponse.error(400, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AlertResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(AlertResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AlertResponse<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(AlertResponse.error(500, "系统异常: " + ex.getMessage()));
    }
}

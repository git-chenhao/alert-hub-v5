package com.alerthub.config;

import com.alerthub.dto.AlertResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AlertResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("参数校验失败: {}", errors);

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(AlertResponse.error(400, "参数校验失败"));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AlertResponse<Void>> handleIllegalArgumentException(
        IllegalArgumentException ex) {
        log.warn("非法参数: {}", ex.getMessage());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(AlertResponse.error(400, ex.getMessage()));
    }

    /**
     * 处理运行时异常
     * 生产环境不返回具体错误信息，避免泄露敏感信息
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AlertResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("运行时异常", ex);

        String message = isProduction() ? "系统内部错误" : "系统内部错误: " + ex.getMessage();

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(AlertResponse.error(500, message));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AlertResponse<Void>> handleException(Exception ex) {
        log.error("未知异常", ex);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(AlertResponse.error(500, "系统内部错误"));
    }

    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }
}

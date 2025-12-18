package com.ql.qlexpress.web.exception;

import com.ql.qlexpress.web.model.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
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
 *
 * @author qlexpress
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TranspileException.class)
    public ResponseEntity<ApiResponse<Object>> handleTranspileException(TranspileException e) {
        log.error("转译错误: {}", e.getMessage(), e);
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .code(e.getCode())
                .message(e.getMessage())
                .data(e.getErrors())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException e) {
        log.error("验证错误: {}", e.getMessage(), e);
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .code(e.getCode())
                .message(e.getMessage())
                .data(e.getErrors())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException e) {
        log.error("业务错误: {}", e.getMessage(), e);
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .code(e.getCode())
                .message(e.getMessage())
                .data(e.getDetails())
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .code("INVALID_REQUEST")
                .message("请求参数验证失败")
                .data(errors)
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception e) {
        log.error("系��错误: {}", e.getMessage(), e);
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .code("INTERNAL_ERROR")
                .message("服务器内部错误")
                .timestamp(System.currentTimeMillis())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

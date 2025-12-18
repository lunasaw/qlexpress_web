package com.ql.qlexpress.web.exception;

import lombok.Getter;

import java.util.List;

/**
 * 验证异常
 *
 * @author qlexpress
 */
@Getter
public class ValidationException extends BusinessException {
    private final List<?> errors;

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
        this.errors = null;
    }

    public ValidationException(String message, List<?> errors) {
        super("VALIDATION_ERROR", message, errors);
        this.errors = errors;
    }
}

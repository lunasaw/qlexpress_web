package com.ql.qlexpress.web.exception;

import lombok.Getter;

import java.util.List;

/**
 * 转译异常
 *
 * @author qlexpress
 */
@Getter
public class TranspileException extends BusinessException {
    private final List<?> errors;

    public TranspileException(String message) {
        super("TRANSPILE_ERROR", message);
        this.errors = null;
    }

    public TranspileException(String message, List<?> errors) {
        super("TRANSPILE_ERROR", message, errors);
        this.errors = errors;
    }

    public TranspileException(String message, Throwable cause) {
        super("TRANSPILE_ERROR", message, cause);
        this.errors = null;
    }
}

package com.ql.qlexpress.web.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author qlexpress
 */
@Getter
public class BusinessException extends RuntimeException {
    private final String code;
    private final Object details;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, Object details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = null;
    }
}

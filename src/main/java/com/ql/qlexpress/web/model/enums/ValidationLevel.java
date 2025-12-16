package com.ql.qlexpress.web.model.enums;

/**
 * 验证级别
 *
 * @author qlexpress
 */
public enum ValidationLevel {
    /**
     * 仅语法验证
     */
    SYNTAX,

    /**
     * 标准验证（语法+结构）
     */
    STANDARD,

    /**
     * 严格验证（语法+结构+类型）
     */
    STRICT
}

package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语义警告
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticWarning {

    /**
     * 行号
     */
    private int line;

    /**
     * 列号
     */
    private int column;

    /**
     * 警告消息
     */
    private String message;

    /**
     * 警告代码
     */
    private String code;

    /**
     * 建议修复
     */
    private String suggestion;

    /**
     * 严重程度
     */
    private WarningSeverity severity;

    public enum WarningSeverity {
        INFO,
        WARNING,
        ERROR
    }
}

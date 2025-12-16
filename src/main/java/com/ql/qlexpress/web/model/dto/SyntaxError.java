package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 语法错误
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyntaxError {

    /**
     * 起始行号
     */
    private int line;

    /**
     * 起始列号
     */
    private int column;

    /**
     * 结束行号
     */
    private int endLine;

    /**
     * 结束列号
     */
    private int endColumn;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误代码
     */
    private String code;

    /**
     * 修复建议
     */
    private String suggestion;

    /**
     * 创建语法错误
     */
    public static SyntaxError of(int line, int column, String message, String code) {
        return SyntaxError.builder()
                .line(line)
                .column(column)
                .endLine(line)
                .endColumn(column)
                .message(message)
                .code(code)
                .build();
    }
}

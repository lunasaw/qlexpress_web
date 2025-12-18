package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 最佳实践建议
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BestPracticeSuggestion {

    /**
     * 建议类型
     */
    private String type;

    /**
     * 行号（可选）
     */
    private Integer line;

    /**
     * 建议消息
     */
    private String message;

    /**
     * 改进建议
     */
    private String suggestion;

    /**
     * 相关代码片段
     */
    private String codeSnippet;

    /**
     * 建议的代码
     */
    private String suggestedCode;
}

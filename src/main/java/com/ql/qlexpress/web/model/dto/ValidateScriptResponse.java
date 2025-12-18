package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证脚本响应
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateScriptResponse {

    /**
     * 是否有效
     */
    private boolean valid;

    /**
     * 语法错误列表
     */
    @Builder.Default
    private List<SyntaxError> syntaxErrors = new ArrayList<>();

    /**
     * 语义警告列表
     */
    @Builder.Default
    private List<SemanticWarning> semanticWarnings = new ArrayList<>();

    /**
     * 最佳实践建议
     */
    @Builder.Default
    private List<BestPracticeSuggestion> suggestions = new ArrayList<>();

    /**
     * 脚本复杂度分析
     */
    private ComplexityAnalysis complexity;

    /**
     * 创建有效响应
     */
    public static ValidateScriptResponse valid() {
        return ValidateScriptResponse.builder()
                .valid(true)
                .build();
    }

    /**
     * 创建有效响应（带复杂度分析）
     */
    public static ValidateScriptResponse valid(ComplexityAnalysis complexity) {
        return ValidateScriptResponse.builder()
                .valid(true)
                .complexity(complexity)
                .build();
    }

    /**
     * 创建无效响应
     */
    public static ValidateScriptResponse invalid(List<SyntaxError> errors) {
        return ValidateScriptResponse.builder()
                .valid(false)
                .syntaxErrors(errors)
                .build();
    }
}

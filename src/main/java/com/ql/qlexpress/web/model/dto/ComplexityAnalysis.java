package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 复杂度分析
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityAnalysis {

    /**
     * 圈复杂度
     */
    private int cyclomaticComplexity;

    /**
     * 最大嵌套深度
     */
    private int maxNestingDepth;

    /**
     * 代码行数
     */
    private int lineCount;

    /**
     * 变量数量
     */
    private int variableCount;

    /**
     * 函数调用数量
     */
    private int functionCallCount;

    /**
     * 循环语句数量
     */
    private int loopCount;

    /**
     * 条件语句数量
     */
    private int conditionCount;

    /**
     * 复杂度等级
     */
    private ComplexityLevel level;

    public enum ComplexityLevel {
        LOW,      // 低复杂度 (1-10)
        MEDIUM,   // 中等复杂度 (11-20)
        HIGH,     // 高复杂度 (21-50)
        VERY_HIGH // 非常高复杂度 (>50)
    }

    /**
     * 计算复杂度等级
     */
    public static ComplexityLevel calculateLevel(int cyclomaticComplexity) {
        if (cyclomaticComplexity <= 10) {
            return ComplexityLevel.LOW;
        } else if (cyclomaticComplexity <= 20) {
            return ComplexityLevel.MEDIUM;
        } else if (cyclomaticComplexity <= 50) {
            return ComplexityLevel.HIGH;
        } else {
            return ComplexityLevel.VERY_HIGH;
        }
    }
}

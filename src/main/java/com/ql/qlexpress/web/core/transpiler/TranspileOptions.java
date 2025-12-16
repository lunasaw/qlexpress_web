package com.ql.qlexpress.web.core.transpiler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转译选项
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranspileOptions {
    /**
     * 是否格式化输出
     */
    @Builder.Default
    private boolean format = true;

    /**
     * 缩进字符串
     */
    @Builder.Default
    private String indent = "    ";

    /**
     * 是否生成注释
     */
    @Builder.Default
    private boolean generateComments = false;

    /**
     * 是否优化代码
     */
    @Builder.Default
    private boolean optimize = false;

    /**
     * 目标语法版本
     */
    @Builder.Default
    private String targetVersion = "4.0";
}

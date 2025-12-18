package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 变量定义
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariableDefinition {
    /**
     * 变量名
     */
    private String name;

    /**
     * 变量类型
     */
    private String type;

    /**
     * 初始值表达式
     */
    private Expression initialValue;

    /**
     * 描述
     */
    private String description;
}

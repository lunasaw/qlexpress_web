package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 参数定义
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterDefinition {
    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型
     */
    private String type;

    /**
     * 默认值表达式
     */
    private Expression defaultValue;

    /**
     * 描述
     */
    private String description;
}

package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 函数定义
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDefinition {
    /**
     * 函数名
     */
    private String name;

    /**
     * 参数列表
     */
    private List<ParameterDefinition> parameters;

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * 函数体入口节点ID
     */
    private String bodyEntrance;

    /**
     * 描述
     */
    private String description;
}

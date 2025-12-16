package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 赋值节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssignmentNodeData extends NodeData {
    /**
     * 变量名
     */
    private String variable;

    /**
     * 变量类型（可选，用于变量声明）
     */
    private String variableType;

    /**
     * 赋值表达式
     */
    private Expression value;
}

package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表达式节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExpressionNodeData extends NodeData {
    /**
     * 表达式
     */
    private Expression expression;

    /**
     * 结果变量名（可选，用于存储表达式结果）
     */
    private String resultVariable;
}

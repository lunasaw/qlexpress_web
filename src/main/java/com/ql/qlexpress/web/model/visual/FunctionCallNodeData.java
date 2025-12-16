package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数调用节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FunctionCallNodeData extends NodeData {
    /**
     * 函数名
     */
    private String functionName;

    /**
     * 参数列表
     */
    private List<Expression> arguments = new ArrayList<>();

    /**
     * 结果变量名（可选，用于存储函数返回值）
     */
    private String resultVariable;

    /**
     * 完整的调用表达式（用于复杂调用如方法链）
     */
    private Expression expression;
}

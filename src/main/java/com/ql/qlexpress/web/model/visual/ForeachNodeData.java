package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Foreach循环节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ForeachNodeData extends NodeData {
    /**
     * 迭代变量名
     */
    private String iteratorVariable;

    /**
     * 可迭代对象表达式
     */
    private Expression iterable;

    /**
     * 循环体入口节点ID
     */
    private String bodyBranch;
}

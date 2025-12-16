package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * IF条件节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IfNodeData extends NodeData {
    /**
     * 条件表达式
     */
    private Expression condition;

    /**
     * true分支入口节点ID
     */
    private String thenBranch;

    /**
     * false分支入口节点ID
     */
    private String elseBranch;
}

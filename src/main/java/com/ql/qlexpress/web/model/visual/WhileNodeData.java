package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * WHILE循环节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WhileNodeData extends NodeData {
    /**
     * 循环条件表达式
     */
    private Expression condition;

    /**
     * 循环体入口节点ID
     */
    private String bodyEntrance;
}

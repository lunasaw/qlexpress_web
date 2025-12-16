package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * FOR循环节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ForNodeData extends NodeData {
    /**
     * 初始化表达式
     */
    private Expression init;

    /**
     * 循环条件表达式
     */
    private Expression condition;

    /**
     * 更新表达式
     */
    private Expression update;

    /**
     * 循环体入口节点ID
     */
    private String bodyEntrance;
}

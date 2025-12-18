package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Throw节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ThrowNodeData extends NodeData {
    /**
     * 抛出的异常表达式
     */
    private Expression exception;
}

package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 返回节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReturnNodeData extends NodeData {
    /**
     * 返回值表达式（可选）
     */
    private Expression value;
}

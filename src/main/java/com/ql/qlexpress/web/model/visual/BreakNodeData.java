package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Break节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BreakNodeData extends NodeData {
    // break语句不需要额外数据
}

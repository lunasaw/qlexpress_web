package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用节点数据（用于存储任意属性）
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GenericNodeData extends NodeData {
    /**
     * 扩展属性
     */
    private Map<String, Object> properties = new HashMap<>();
}

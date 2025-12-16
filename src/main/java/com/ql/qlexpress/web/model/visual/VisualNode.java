package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可视化节点
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualNode {
    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点类型
     */
    private String type;

    /**
     * 节点位置
     */
    private Position position;

    /**
     * 节点数据
     */
    private NodeData data;

    /**
     * 节点样式
     */
    private NodeStyle style;

    /**
     * 是否选中
     */
    private Boolean selected;

    /**
     * 是否正在拖拽
     */
    private Boolean dragging;
}

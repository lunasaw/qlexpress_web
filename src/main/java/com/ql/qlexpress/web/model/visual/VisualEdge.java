package com.ql.qlexpress.web.model.visual;

import com.ql.qlexpress.web.model.enums.EdgeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 可视化连线
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualEdge {
    /**
     * 连线ID
     */
    private String id;

    /**
     * 源节点ID
     */
    private String source;

    /**
     * 目标节点ID
     */
    private String target;

    /**
     * 源连接点ID
     */
    private String sourceHandle;

    /**
     * 目标连接点ID
     */
    private String targetHandle;

    /**
     * 连线类型
     */
    private EdgeType type;

    /**
     * 连线标签
     */
    private String label;

    /**
     * 连线数据
     */
    private EdgeData data;

    /**
     * 连线样式
     */
    private EdgeStyle style;

    /**
     * 是否动画
     */
    private Boolean animated;
}

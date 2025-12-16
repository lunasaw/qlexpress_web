package com.ql.qlexpress.web.model.enums;

/**
 * 流程错误类型
 *
 * @author qlexpress
 */
public enum FlowErrorType {
    /**
     * 无起始节点
     */
    NO_START_NODE,

    /**
     * 无结束节点
     */
    NO_END_NODE,

    /**
     * 多个起始节点
     */
    MULTIPLE_START_NODES,

    /**
     * 存在不可达节点
     */
    UNREACHABLE_NODES,

    /**
     * 存在无限循环
     */
    INFINITE_LOOP,

    /**
     * 图不连通
     */
    DISCONNECTED_GRAPH,

    /**
     * 节点ID重复
     */
    DUPLICATE_NODE_ID,

    /**
     * 边ID重复
     */
    DUPLICATE_EDGE_ID,

    /**
     * 无效的边（源或目标节点不存在）
     */
    INVALID_EDGE,

    /**
     * 空流程
     */
    EMPTY_FLOW
}

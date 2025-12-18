package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点错误
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeError {

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 错误代码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 属性名称（如果是属性错误）
     */
    private String propertyName;

    /**
     * 创建节点错误
     */
    public static NodeError of(String nodeId, String nodeType, String code, String message) {
        return NodeError.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建属性错误
     */
    public static NodeError propertyError(String nodeId, String nodeType, String propertyName, String message) {
        return NodeError.builder()
                .nodeId(nodeId)
                .nodeType(nodeType)
                .code("INVALID_PROPERTY")
                .propertyName(propertyName)
                .message(message)
                .build();
    }
}

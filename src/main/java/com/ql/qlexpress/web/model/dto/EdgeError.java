package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连线错误
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeError {

    /**
     * 边ID
     */
    private String edgeId;

    /**
     * 源节点ID
     */
    private String sourceNodeId;

    /**
     * 目标节点ID
     */
    private String targetNodeId;

    /**
     * 错误代码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 创建边错误
     */
    public static EdgeError of(String edgeId, String sourceNodeId, String targetNodeId,
                               String code, String message) {
        return EdgeError.builder()
                .edgeId(edgeId)
                .sourceNodeId(sourceNodeId)
                .targetNodeId(targetNodeId)
                .code(code)
                .message(message)
                .build();
    }
}

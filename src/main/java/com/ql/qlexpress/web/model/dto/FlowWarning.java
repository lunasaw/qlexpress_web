package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程警告
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowWarning {

    /**
     * 警告代码
     */
    private String code;

    /**
     * 警告消息
     */
    private String message;

    /**
     * 相关节点ID（如果有）
     */
    private String nodeId;

    /**
     * 建议
     */
    private String suggestion;

    /**
     * 创建警告
     */
    public static FlowWarning of(String code, String message) {
        return FlowWarning.builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 创建节点相关警告
     */
    public static FlowWarning forNode(String nodeId, String code, String message) {
        return FlowWarning.builder()
                .nodeId(nodeId)
                .code(code)
                .message(message)
                .build();
    }
}

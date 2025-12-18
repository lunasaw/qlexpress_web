package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 类型错误
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeError {

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 属性名称
     */
    private String propertyName;

    /**
     * 期望类型
     */
    private String expectedType;

    /**
     * 实际类型
     */
    private String actualType;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 创建类型错误
     */
    public static TypeError of(String nodeId, String propertyName,
                               String expectedType, String actualType) {
        return TypeError.builder()
                .nodeId(nodeId)
                .propertyName(propertyName)
                .expectedType(expectedType)
                .actualType(actualType)
                .message(String.format("类型不匹配：期望 %s，实际 %s", expectedType, actualType))
                .build();
    }
}

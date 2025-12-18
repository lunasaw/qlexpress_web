package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.FlowErrorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程结构错误
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowError {

    /**
     * 错误代码
     */
    private String code;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误类型
     */
    private FlowErrorType type;

    /**
     * 创建错误
     */
    public static FlowError of(FlowErrorType type, String message) {
        return FlowError.builder()
                .code(type.name())
                .type(type)
                .message(message)
                .build();
    }
}

package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证规则
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRule {
    /**
     * 规则类型
     */
    private String type;

    /**
     * 规则值
     */
    private Object value;

    /**
     * 错误消息
     */
    private String message;
}

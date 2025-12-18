package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 函数示例
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionExample {
    /**
     * 示例代码
     */
    private String code;

    /**
     * 输入参数
     */
    private Map<String, Object> input;

    /**
     * 预期输出
     */
    private Object expectedOutput;

    /**
     * 说明
     */
    private String description;
}

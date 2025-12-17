package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 直接执行EL表达式请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteELRequest {

    /**
     * EL表达式
     * 示例: THEN(a, b, c)
     */
    @NotBlank(message = "EL表达式不能为空")
    private String el;

    /**
     * 执行参数 (可选)
     */
    private Map<String, Object> params;
}

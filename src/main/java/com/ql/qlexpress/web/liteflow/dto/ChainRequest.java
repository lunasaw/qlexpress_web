package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 流程链请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainRequest {

    /**
     * 流程链名称 (唯一标识)
     */
    @NotBlank(message = "流程链名称不能为空")
    private String chainName;

    /**
     * EL表达式
     * 示例: THEN(a, b, c) 或 THEN(a, WHEN(b, c), d)
     */
    @NotBlank(message = "EL表达式不能为空")
    private String el;
}

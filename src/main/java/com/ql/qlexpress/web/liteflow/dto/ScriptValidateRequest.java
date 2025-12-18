package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 脚本验证请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptValidateRequest {

    /**
     * 脚本内容
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;

    /**
     * 脚本语言
     */
    @Builder.Default
    private String language = "qlexpress";
}

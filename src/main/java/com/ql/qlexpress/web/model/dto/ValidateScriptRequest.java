package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 验证脚本请求
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateScriptRequest {

    /**
     * QL脚本
     */
    @NotBlank(message = "脚本不能为空")
    private String script;

    /**
     * 是否检查语义
     */
    @Builder.Default
    private boolean checkSemantics = true;

    /**
     * 是否检查最佳实践
     */
    @Builder.Default
    private boolean checkBestPractices = false;
}

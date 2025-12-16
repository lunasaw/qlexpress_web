package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 执行脚本请求
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteScriptRequest {

    /**
     * QL脚本
     */
    @NotBlank(message = "脚本不能为空")
    private String script;

    /**
     * 执行上下文（变量）
     */
    private Map<String, Object> context;

    /**
     * 执行选项
     */
    private ExecuteOptions options;
}

package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 脚本节点请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScriptNodeRequest {

    /**
     * 节点ID (唯一标识)
     */
    @NotBlank(message = "节点ID不能为空")
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 脚本内容
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;

    /**
     * 节点类型: script, boolean_script, switch_script
     * 默认: script
     */
    private String nodeType = "script";

    /**
     * 脚本语言: qlexpress, groovy, javascript, python, lua, aviator, kotlin
     * 默认: qlexpress
     */
    private String language = "qlexpress";
}

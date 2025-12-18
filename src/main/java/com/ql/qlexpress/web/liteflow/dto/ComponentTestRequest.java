package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 组件测试请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentTestRequest {

    /**
     * 测试参数
     */
    private Map<String, Object> params;

    /**
     * 脚本内容 (用于直接测试脚本，不需要先保存组件)
     */
    private String script;

    /**
     * 脚本语言
     */
    @Builder.Default
    private String language = "qlexpress";
}

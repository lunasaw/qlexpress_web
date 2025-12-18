package com.ql.qlexpress.web.liteflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 流程转换结果
 * 包含 LiteFlow 所需的节点定义和 EL 表达式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertResult {

    /**
     * 流程ID
     */
    private String flowId;

    /**
     * 流程链名称 (LiteFlow chainName)
     */
    private String chainName;

    /**
     * EL 表达式
     */
    private String el;

    /**
     * 节点定义列表
     */
    @Builder.Default
    private List<NodeDefinition> nodes = new ArrayList<>();

    /**
     * 使用的组件ID列表
     */
    @Builder.Default
    private Set<String> usedComponentIds = new HashSet<>();

    /**
     * 转换是否成功
     */
    @Builder.Default
    private boolean success = true;

    /**
     * 错误信息 (如果转换失败)
     */
    private String errorMessage;

    /**
     * 警告信息列表
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * 节点定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDefinition {
        /**
         * 节点ID
         */
        private String nodeId;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 节点类型: script, boolean_script, switch_script
         */
        private String nodeType;

        /**
         * 脚本语言
         */
        @Builder.Default
        private String language = "qlexpress";

        /**
         * 脚本内容
         */
        private String script;
    }

    /**
     * 添加警告
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    /**
     * 设置失败状态
     */
    public void setFailed(String error) {
        this.success = false;
        this.errorMessage = error;
    }
}

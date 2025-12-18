package com.ql.qlexpress.web.liteflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * QLExpress 组件定义模型
 * 每个组件对应一个 LiteFlow 节点，内部使用 QLExpress 脚本实现逻辑
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QLComponent {

    /**
     * 组件唯一标识 (对应 LiteFlow nodeId)
     */
    @NotBlank(message = "组件ID不能为空")
    private String componentId;

    /**
     * 组件名称 (显示用)
     */
    @NotBlank(message = "组件名称不能为空")
    private String componentName;

    /**
     * 组件描述
     */
    private String description;

    /**
     * 组件分类
     */
    private String category;

    /**
     * 组件图标 (可选，用于前端显示)
     */
    private String icon;

    /**
     * 组件类型
     * - SCRIPT: 普通脚本节点，执行逻辑无返回
     * - BOOLEAN: 条件判断节点，返回 true/false
     * - SWITCH: 路由选择节点，返回分支标识
     */
    @NotNull(message = "组件类型不能为空")
    private ComponentType componentType;

    /**
     * QLExpress 脚本内容
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;

    /**
     * 脚本语言 (默认 qlexpress)
     */
    @Builder.Default
    private String language = "qlexpress";

    /**
     * 输入参数定义
     */
    @Valid
    private List<ParameterDefinition> inputs;

    /**
     * 输出参数定义 (SCRIPT 类型使用)
     */
    @Valid
    private List<ParameterDefinition> outputs;

    /**
     * Switch 分支定义 (仅 SWITCH 类型使用)
     * 定义该组件可能返回的所有分支标识
     */
    private List<SwitchBranch> switchBranches;

    /**
     * 默认分支 (SWITCH 类型，当返回值不匹配任何分支时使用)
     */
    private String defaultBranch;

    /**
     * 组件版本
     */
    @Builder.Default
    private String version = "1.0.0";

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 组件类型枚举
     */
    public enum ComponentType {
        /**
         * 普通脚本节点 - 执行业务逻辑，可设置上下文变量
         */
        SCRIPT("script"),

        /**
         * 布尔判断节点 - 返回 true/false，用于 IF 条件
         */
        BOOLEAN("boolean_script"),

        /**
         * 路由选择节点 - 返回分支标识字符串，用于 SWITCH
         */
        SWITCH("switch_script");

        private final String liteFlowNodeType;

        ComponentType(String liteFlowNodeType) {
            this.liteFlowNodeType = liteFlowNodeType;
        }

        public String getLiteFlowNodeType() {
            return liteFlowNodeType;
        }
    }

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParameterDefinition {
        /**
         * 参数名称
         */
        @NotBlank(message = "参数名称不能为空")
        private String name;

        /**
         * 参数类型
         */
        @NotBlank(message = "参数类型不能为空")
        private String type;

        /**
         * 参数描述
         */
        private String description;

        /**
         * 是否必填
         */
        @Builder.Default
        private boolean required = true;

        /**
         * 默认值
         */
        private Object defaultValue;

        /**
         * 验证规则 (可选)
         */
        private String validation;
    }

    /**
     * Switch 分支定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SwitchBranch {
        /**
         * 分支标识 (脚本返回值)
         */
        @NotBlank(message = "分支标识不能为空")
        private String branchId;

        /**
         * 分支名称 (显示用)
         */
        @NotBlank(message = "分支名称不能为空")
        private String branchName;

        /**
         * 分支描述
         */
        private String description;

        /**
         * 分支图标或颜色 (可选)
         */
        private String style;
    }

    /**
     * 转换为 LiteFlow 节点类型字符串
     */
    public String toLiteFlowNodeType() {
        return componentType != null ? componentType.getLiteFlowNodeType() : "script";
    }

    /**
     * 获取 Switch 分支数量
     */
    public int getSwitchBranchCount() {
        return switchBranches != null ? switchBranches.size() : 0;
    }
}

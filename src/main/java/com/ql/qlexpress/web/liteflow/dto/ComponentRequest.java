package com.ql.qlexpress.web.liteflow.dto;

import com.ql.qlexpress.web.liteflow.model.QLComponent;
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
 * 组件请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentRequest {

    /**
     * 组件唯一标识
     */
    @NotBlank(message = "组件ID不能为空")
    private String componentId;

    /**
     * 组件名称
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
     * 组件图标
     */
    private String icon;

    /**
     * 组件类型
     */
    @NotNull(message = "组件类型不能为空")
    private QLComponent.ComponentType componentType;

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

    /**
     * 输入参数定义
     */
    @Valid
    private List<QLComponent.ParameterDefinition> inputs;

    /**
     * 输出参数定义
     */
    @Valid
    private List<QLComponent.ParameterDefinition> outputs;

    /**
     * Switch 分支定义
     */
    private List<QLComponent.SwitchBranch> switchBranches;

    /**
     * 默认分支
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
     * 转换为 QLComponent 实体
     */
    public QLComponent toEntity() {
        return QLComponent.builder()
                .componentId(componentId)
                .componentName(componentName)
                .description(description)
                .category(category)
                .icon(icon)
                .componentType(componentType)
                .script(script)
                .language(language != null ? language : "qlexpress")
                .inputs(inputs)
                .outputs(outputs)
                .switchBranches(switchBranches)
                .defaultBranch(defaultBranch)
                .version(version != null ? version : "1.0.0")
                .enabled(enabled)
                .metadata(metadata)
                .build();
    }
}

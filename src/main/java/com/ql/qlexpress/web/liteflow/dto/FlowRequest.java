package com.ql.qlexpress.web.liteflow.dto;

import com.ql.qlexpress.web.liteflow.model.CmpProperty;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 流程请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowRequest {

    /**
     * 流程唯一标识
     */
    @NotBlank(message = "流程ID不能为空")
    private String flowId;

    /**
     * 流程名称
     */
    @NotBlank(message = "流程名称不能为空")
    private String flowName;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 流程分类
     */
    private String category;

    /**
     * 流程版本
     */
    @Builder.Default
    private String version = "1.0.0";

    /**
     * 是否启用
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 流程编排树
     */
    @NotNull(message = "流程编排不能为空")
    @Valid
    private CmpProperty root;

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 转换为 FlowDesign 实体
     */
    public FlowDesign toEntity() {
        return FlowDesign.builder()
                .flowId(flowId)
                .flowName(flowName)
                .description(description)
                .category(category)
                .version(version != null ? version : "1.0.0")
                .enabled(enabled)
                .root(root)
                .metadata(metadata)
                .build();
    }
}

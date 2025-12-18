package com.ql.qlexpress.web.liteflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程设计模型
 * 包含流程的完整定义，用于可视化编排和持久化
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowDesign {

    // ==================== 基础信息 ====================

    /**
     * 流程唯一标识 (对应 LiteFlow chainId)
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

    // ==================== 编排定义 ====================

    /**
     * 流程编排树 (根节点)
     */
    @NotNull(message = "流程编排不能为空")
    @Valid
    private CmpProperty root;

    // ==================== 组件引用 ====================

    /**
     * 流程使用的组件ID列表 (自动收集)
     */
    private Set<String> usedComponentIds;

    // ==================== 元数据 ====================

    /**
     * 扩展属性
     */
    private Map<String, Object> metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建者
     */
    private String createdBy;

    /**
     * 更新者
     */
    private String updatedBy;

    // ==================== 部署状态 ====================

    /**
     * 部署状态
     */
    @Builder.Default
    private DeployStatus deployStatus = DeployStatus.NOT_DEPLOYED;

    /**
     * 上次部署时间
     */
    private LocalDateTime lastDeployTime;

    /**
     * 部署状态枚举
     */
    public enum DeployStatus {
        NOT_DEPLOYED,   // 未部署
        DEPLOYED,       // 已部署
        MODIFIED,       // 已修改 (需要重新部署)
        FAILED          // 部署失败
    }
}

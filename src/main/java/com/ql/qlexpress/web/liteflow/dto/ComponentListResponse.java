package com.ql.qlexpress.web.liteflow.dto;

import com.ql.qlexpress.web.liteflow.model.QLComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 组件列表响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentListResponse {

    /**
     * 总数
     */
    private long total;

    /**
     * 所有分类
     */
    private Set<String> categories;

    /**
     * 组件列表
     */
    private List<ComponentSummary> components;

    /**
     * 分类统计
     */
    private Map<String, Integer> categoryStats;

    /**
     * 组件摘要信息 (不包含脚本内容)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentSummary {
        private String componentId;
        private String componentName;
        private String description;
        private String category;
        private String icon;
        private QLComponent.ComponentType componentType;
        private String version;
        private boolean enabled;
        private int inputCount;
        private int outputCount;
        private int switchBranchCount;

        /**
         * 从 QLComponent 创建摘要
         */
        public static ComponentSummary fromEntity(QLComponent component) {
            return ComponentSummary.builder()
                    .componentId(component.getComponentId())
                    .componentName(component.getComponentName())
                    .description(component.getDescription())
                    .category(component.getCategory())
                    .icon(component.getIcon())
                    .componentType(component.getComponentType())
                    .version(component.getVersion())
                    .enabled(component.isEnabled())
                    .inputCount(component.getInputs() != null ? component.getInputs().size() : 0)
                    .outputCount(component.getOutputs() != null ? component.getOutputs().size() : 0)
                    .switchBranchCount(component.getSwitchBranchCount())
                    .build();
        }
    }
}

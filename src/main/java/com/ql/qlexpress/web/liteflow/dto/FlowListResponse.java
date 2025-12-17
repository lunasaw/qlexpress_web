package com.ql.qlexpress.web.liteflow.dto;

import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 流程列表响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowListResponse {

    /**
     * 总数
     */
    private long total;

    /**
     * 所有分类
     */
    private Set<String> categories;

    /**
     * 流程列表
     */
    private List<FlowSummary> flows;

    /**
     * 分类统计
     */
    private Map<String, Integer> categoryStats;

    /**
     * 流程摘要信息 (不包含完整的编排树)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlowSummary {
        private String flowId;
        private String flowName;
        private String description;
        private String category;
        private String version;
        private boolean enabled;
        private FlowDesign.DeployStatus deployStatus;
        private int componentCount;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
        private LocalDateTime lastDeployTime;

        /**
         * 从 FlowDesign 创建摘要
         */
        public static FlowSummary fromEntity(FlowDesign flow) {
            return FlowSummary.builder()
                    .flowId(flow.getFlowId())
                    .flowName(flow.getFlowName())
                    .description(flow.getDescription())
                    .category(flow.getCategory())
                    .version(flow.getVersion())
                    .enabled(flow.isEnabled())
                    .deployStatus(flow.getDeployStatus())
                    .componentCount(flow.getUsedComponentIds() != null ? flow.getUsedComponentIds().size() : 0)
                    .createTime(flow.getCreateTime())
                    .updateTime(flow.getUpdateTime())
                    .lastDeployTime(flow.getLastDeployTime())
                    .build();
        }
    }
}

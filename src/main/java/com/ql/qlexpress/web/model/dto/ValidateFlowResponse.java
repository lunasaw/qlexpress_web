package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 验证流程响应
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateFlowResponse {

    /**
     * 是否有效
     */
    private boolean valid;

    /**
     * 结构错误
     */
    @Builder.Default
    private List<FlowError> structureErrors = new ArrayList<>();

    /**
     * 节点错误
     */
    @Builder.Default
    private List<NodeError> nodeErrors = new ArrayList<>();

    /**
     * 连线错误
     */
    @Builder.Default
    private List<EdgeError> edgeErrors = new ArrayList<>();

    /**
     * 类型错误
     */
    @Builder.Default
    private List<TypeError> typeErrors = new ArrayList<>();

    /**
     * 警告
     */
    @Builder.Default
    private List<FlowWarning> warnings = new ArrayList<>();

    /**
     * 创建有效响应
     */
    public static ValidateFlowResponse valid() {
        return ValidateFlowResponse.builder()
                .valid(true)
                .build();
    }

    /**
     * 创建有效响应（带警告）
     */
    public static ValidateFlowResponse validWithWarnings(List<FlowWarning> warnings) {
        return ValidateFlowResponse.builder()
                .valid(true)
                .warnings(warnings)
                .build();
    }

    /**
     * 创建无效响应
     */
    public static ValidateFlowResponse invalid() {
        return ValidateFlowResponse.builder()
                .valid(false)
                .build();
    }

    /**
     * 判断是否有错误
     */
    public boolean hasErrors() {
        return !structureErrors.isEmpty()
                || !nodeErrors.isEmpty()
                || !edgeErrors.isEmpty()
                || !typeErrors.isEmpty();
    }

    /**
     * 获取总错误数
     */
    public int getErrorCount() {
        return structureErrors.size()
                + nodeErrors.size()
                + edgeErrors.size()
                + typeErrors.size();
    }
}

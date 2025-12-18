package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 执行可视化流程请求
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteFlowRequest {

    /**
     * 流程定义
     */
    @NotNull(message = "流程定义不能为空")
    private VisualFlowSchema flow;

    /**
     * 执行上下文
     */
    private Map<String, Object> context;

    /**
     * 执行选项
     */
    private ExecuteOptions options;

    /**
     * 是否返回节点执行详情
     */
    @Builder.Default
    private boolean includeNodeDetails = false;
}

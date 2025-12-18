package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.core.transpiler.TranspileOptions;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 可视化JSON转QL脚本请求
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranspileToQLRequest {

    /**
     * 流程定义
     */
    @NotNull(message = "流程定义不能为空")
    private VisualFlowSchema flow;

    /**
     * 转译选项
     */
    private TranspileOptions options;
}

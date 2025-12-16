package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.ValidationLevel;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

/**
 * 验证流程请求
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateFlowRequest {

    /**
     * 流程定义
     */
    @NotNull(message = "流程定义不能为空")
    private VisualFlowSchema flow;

    /**
     * 验证级别
     */
    @Builder.Default
    private ValidationLevel level = ValidationLevel.STANDARD;
}

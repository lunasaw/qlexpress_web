package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 解析QL脚本为可视化流程请求
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseToVisualRequest {

    /**
     * QL脚本内容
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;

    /**
     * 是否保留注释
     */
    @Builder.Default
    private boolean preserveComments = false;

    /**
     * 是否优化布局
     */
    @Builder.Default
    private boolean optimizeLayout = true;
}

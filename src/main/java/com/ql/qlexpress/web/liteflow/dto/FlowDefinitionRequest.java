package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import java.util.List;

/**
 * 完整流程定义请求DTO
 * 包含节点定义和流程链定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowDefinitionRequest {

    /**
     * 脚本节点列表
     */
    @Valid
    private List<ScriptNodeRequest> nodes;

    /**
     * 流程链列表
     */
    @Valid
    private List<ChainRequest> chains;
}

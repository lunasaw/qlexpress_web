package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 可视化流程定义
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisualFlowSchema {
    /**
     * 版本号
     */
    private String version;

    /**
     * 流程元数据
     */
    private FlowMetadata metadata;

    /**
     * 导入列表
     */
    @Builder.Default
    private List<String> imports = new ArrayList<>();

    /**
     * 节点列表
     */
    @Builder.Default
    private List<VisualNode> nodes = new ArrayList<>();

    /**
     * 连线列表
     */
    @Builder.Default
    private List<VisualEdge> edges = new ArrayList<>();

    /**
     * 变量定义列表
     */
    @Builder.Default
    private List<VariableDefinition> variables = new ArrayList<>();

    /**
     * 函数定义列表
     */
    @Builder.Default
    private List<FunctionDefinition> functions = new ArrayList<>();
}

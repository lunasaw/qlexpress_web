package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 可视化流程定义
 * 支持完整的QL脚本结构：imports、函数定义、主流程
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
    @Builder.Default
    private String version = "1.0";

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
     * 节点列表 (主流程)
     */
    @Builder.Default
    private List<VisualNode> nodes = new ArrayList<>();

    /**
     * 连线列表 (主流程)
     */
    @Builder.Default
    private List<VisualEdge> edges = new ArrayList<>();

    /**
     * 变量定义列表 (全局变量)
     */
    @Builder.Default
    private List<VariableDefinition> variables = new ArrayList<>();

    /**
     * 函数定义列表
     */
    @Builder.Default
    private List<FunctionDefinition> functions = new ArrayList<>();

    // ========== 辅助方法 ==========

    /**
     * 添加导入
     */
    public void addImport(String importStr) {
        if (imports == null) {
            imports = new ArrayList<>();
        }
        if (!imports.contains(importStr)) {
            imports.add(importStr);
        }
    }

    /**
     * 添加节点
     */
    public void addNode(VisualNode node) {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(node);
    }

    /**
     * 添加边
     */
    public void addEdge(VisualEdge edge) {
        if (edges == null) {
            edges = new ArrayList<>();
        }
        edges.add(edge);
    }

    /**
     * 添加变量定义
     */
    public void addVariable(VariableDefinition variable) {
        if (variables == null) {
            variables = new ArrayList<>();
        }
        variables.add(variable);
    }

    /**
     * 添加函数定义
     */
    public void addFunction(FunctionDefinition function) {
        if (functions == null) {
            functions = new ArrayList<>();
        }
        functions.add(function);
    }

    /**
     * 根据ID查找节点
     */
    public Optional<VisualNode> findNodeById(String nodeId) {
        if (nodes == null || nodeId == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst();
    }

    /**
     * 查找起始节点
     */
    public Optional<VisualNode> findStartNode() {
        if (nodes == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(n -> "start".equals(n.getType()))
                .findFirst();
    }

    /**
     * 查找结束节点
     */
    public Optional<VisualNode> findEndNode() {
        if (nodes == null) {
            return Optional.empty();
        }
        return nodes.stream()
                .filter(n -> "end".equals(n.getType()))
                .findFirst();
    }

    /**
     * 根据名称查找函数定义
     */
    public Optional<FunctionDefinition> findFunctionByName(String name) {
        if (functions == null || name == null) {
            return Optional.empty();
        }
        return functions.stream()
                .filter(f -> name.equals(f.getName()))
                .findFirst();
    }

    /**
     * 查找从指定节点出发的边
     */
    public List<VisualEdge> findEdgesFromNode(String nodeId) {
        if (edges == null || nodeId == null) {
            return new ArrayList<>();
        }
        List<VisualEdge> result = new ArrayList<>();
        for (VisualEdge edge : edges) {
            if (nodeId.equals(edge.getSource())) {
                result.add(edge);
            }
        }
        return result;
    }

    /**
     * 查找指向指定节点的边
     */
    public List<VisualEdge> findEdgesToNode(String nodeId) {
        if (edges == null || nodeId == null) {
            return new ArrayList<>();
        }
        List<VisualEdge> result = new ArrayList<>();
        for (VisualEdge edge : edges) {
            if (nodeId.equals(edge.getTarget())) {
                result.add(edge);
            }
        }
        return result;
    }

    /**
     * 检查是否有导入
     */
    public boolean hasImports() {
        return imports != null && !imports.isEmpty();
    }

    /**
     * 检查是否有函数定义
     */
    public boolean hasFunctions() {
        return functions != null && !functions.isEmpty();
    }
}

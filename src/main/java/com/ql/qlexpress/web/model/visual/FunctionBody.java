package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数体结构
 * 包含函数内部的节点和边，形成独立的子流程图
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionBody {

    /**
     * 入口节点ID
     */
    private String entranceNodeId;

    /**
     * 函数体内部节点列表
     */
    @Builder.Default
    private List<VisualNode> nodes = new ArrayList<>();

    /**
     * 函数体内部边列表
     */
    @Builder.Default
    private List<VisualEdge> edges = new ArrayList<>();

    /**
     * 局部变量定义
     */
    @Builder.Default
    private List<VariableDefinition> localVariables = new ArrayList<>();

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
     * 添加局部变量
     */
    public void addLocalVariable(VariableDefinition variable) {
        if (localVariables == null) {
            localVariables = new ArrayList<>();
        }
        localVariables.add(variable);
    }

    /**
     * 根据ID查找节点
     */
    public VisualNode findNodeById(String nodeId) {
        if (nodes == null || nodeId == null) {
            return null;
        }
        return nodes.stream()
                .filter(n -> nodeId.equals(n.getId()))
                .findFirst()
                .orElse(null);
    }
}

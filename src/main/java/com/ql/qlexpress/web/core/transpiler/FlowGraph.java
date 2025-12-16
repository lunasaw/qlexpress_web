package com.ql.qlexpress.web.core.transpiler;

import com.ql.qlexpress.web.model.visual.VisualEdge;
import com.ql.qlexpress.web.model.visual.VisualNode;
import lombok.Getter;

import java.util.*;

/**
 * 流程图结构
 *
 * @author qlexpress
 */
@Getter
public class FlowGraph {
    /**
     * 节点映射（ID -> 节点）
     */
    private final Map<String, VisualNode> nodes;

    /**
     * 出边映射（节点ID -> 出边列表）
     */
    private final Map<String, List<VisualEdge>> outEdges;

    /**
     * 入边映射（节点ID -> 入边列表）
     */
    private final Map<String, List<VisualEdge>> inEdges;

    /**
     * 起始节点
     */
    private VisualNode startNode;

    /**
     * 结束节点列表
     */
    private final List<VisualNode> endNodes;

    public FlowGraph() {
        this.nodes = new HashMap<>();
        this.outEdges = new HashMap<>();
        this.inEdges = new HashMap<>();
        this.endNodes = new ArrayList<>();
    }

    /**
     * 从节点和边列表构建图
     */
    public static FlowGraph build(List<VisualNode> nodeList, List<VisualEdge> edgeList) {
        FlowGraph graph = new FlowGraph();

        // 添加节点
        for (VisualNode node : nodeList) {
            graph.addNode(node);
        }

        // 添加边
        for (VisualEdge edge : edgeList) {
            graph.addEdge(edge);
        }

        return graph;
    }

    /**
     * 添加节点
     */
    public void addNode(VisualNode node) {
        nodes.put(node.getId(), node);
        outEdges.putIfAbsent(node.getId(), new ArrayList<>());
        inEdges.putIfAbsent(node.getId(), new ArrayList<>());

        // 识别起始和结束节点
        if ("start".equals(node.getType())) {
            this.startNode = node;
        } else if ("end".equals(node.getType())) {
            this.endNodes.add(node);
        }
    }

    /**
     * 添加边
     */
    public void addEdge(VisualEdge edge) {
        outEdges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        inEdges.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge);
    }

    /**
     * 获取节点
     */
    public VisualNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * 获取节点的出边
     */
    public List<VisualEdge> getOutEdges(String nodeId) {
        return outEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 获取节点的入边
     */
    public List<VisualEdge> getInEdges(String nodeId) {
        return inEdges.getOrDefault(nodeId, Collections.emptyList());
    }

    /**
     * 获取节点的后继节点
     */
    public List<VisualNode> getSuccessors(String nodeId) {
        List<VisualNode> successors = new ArrayList<>();
        for (VisualEdge edge : getOutEdges(nodeId)) {
            VisualNode target = getNode(edge.getTarget());
            if (target != null) {
                successors.add(target);
            }
        }
        return successors;
    }

    /**
     * 获取节点的前驱节点
     */
    public List<VisualNode> getPredecessors(String nodeId) {
        List<VisualNode> predecessors = new ArrayList<>();
        for (VisualEdge edge : getInEdges(nodeId)) {
            VisualNode source = getNode(edge.getSource());
            if (source != null) {
                predecessors.add(source);
            }
        }
        return predecessors;
    }

    /**
     * 按sourceHandle获取特定的出边
     */
    public VisualEdge getOutEdgeByHandle(String nodeId, String sourceHandle) {
        for (VisualEdge edge : getOutEdges(nodeId)) {
            if (Objects.equals(sourceHandle, edge.getSourceHandle())) {
                return edge;
            }
        }
        return null;
    }

    /**
     * 按sourceHandle获取后继节点
     */
    public VisualNode getSuccessorByHandle(String nodeId, String sourceHandle) {
        VisualEdge edge = getOutEdgeByHandle(nodeId, sourceHandle);
        return edge != null ? getNode(edge.getTarget()) : null;
    }

    /**
     * 检查是否有起始节点
     */
    public boolean hasStartNode() {
        return startNode != null;
    }

    /**
     * 检查是否有结束节点
     */
    public boolean hasEndNode() {
        return !endNodes.isEmpty();
    }

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取边数量
     */
    public int getEdgeCount() {
        return outEdges.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 获取所有节点
     */
    public Collection<VisualNode> getAllNodes() {
        return nodes.values();
    }

    /**
     * 查找从起始节点可达的所有节点
     */
    public Set<String> findReachableNodes() {
        Set<String> reachable = new HashSet<>();
        if (startNode == null) {
            return reachable;
        }

        Queue<String> queue = new LinkedList<>();
        queue.offer(startNode.getId());
        reachable.add(startNode.getId());

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            for (VisualEdge edge : getOutEdges(nodeId)) {
                if (!reachable.contains(edge.getTarget())) {
                    reachable.add(edge.getTarget());
                    queue.offer(edge.getTarget());
                }
            }
        }

        return reachable;
    }

    /**
     * 拓扑排序
     */
    public List<VisualNode> topologicalSort() {
        List<VisualNode> result = new ArrayList<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // 初始化入度
        for (String nodeId : nodes.keySet()) {
            inDegree.put(nodeId, getInEdges(nodeId).size());
        }

        // 找出入度为0的节点
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // BFS处理
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            VisualNode node = getNode(nodeId);
            if (node != null) {
                result.add(node);
            }

            for (VisualEdge edge : getOutEdges(nodeId)) {
                String targetId = edge.getTarget();
                int newDegree = inDegree.get(targetId) - 1;
                inDegree.put(targetId, newDegree);
                if (newDegree == 0) {
                    queue.offer(targetId);
                }
            }
        }

        return result;
    }

    /**
     * 检测是否有循环
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeId : nodes.keySet()) {
            if (hasCycleUtil(nodeId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCycleUtil(String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        for (VisualEdge edge : getOutEdges(nodeId)) {
            if (hasCycleUtil(edge.getTarget(), visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }
}

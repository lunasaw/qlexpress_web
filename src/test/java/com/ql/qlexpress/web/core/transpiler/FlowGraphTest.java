package com.ql.qlexpress.web.core.transpiler;

import com.ql.qlexpress.web.model.enums.EdgeType;
import com.ql.qlexpress.web.model.visual.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowGraph 单元测试
 *
 * @author qlexpress
 */
class FlowGraphTest {

    @Nested
    @DisplayName("图构建测试")
    class GraphBuildTest {

        @Test
        @DisplayName("应正确构建图结构")
        void build_shouldCreateCorrectGraph() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n2", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            assertEquals(3, graph.getNodeCount());
            assertEquals(2, graph.getEdgeCount());
            assertNotNull(graph.getNode("n1"));
            assertNotNull(graph.getNode("n2"));
            assertNotNull(graph.getNode("n3"));
        }

        @Test
        @DisplayName("应正确识别起始节点")
        void build_shouldIdentifyStartNode() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "end")
            );
            List<VisualEdge> edges = Collections.singletonList(
                    createEdge("e1", "n1", "n2")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            assertTrue(graph.hasStartNode());
            assertEquals("n1", graph.getStartNode().getId());
        }

        @Test
        @DisplayName("应正确识别结束节点")
        void build_shouldIdentifyEndNodes() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "end"),
                    createNode("n3", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n1", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            assertTrue(graph.hasEndNode());
            assertEquals(2, graph.getEndNodes().size());
        }
    }

    @Nested
    @DisplayName("边查询测试")
    class EdgeQueryTest {

        @Test
        @DisplayName("应正确获取出边")
        void getOutEdges_shouldReturnCorrectEdges() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n1", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            List<VisualEdge> outEdges = graph.getOutEdges("n1");
            assertEquals(2, outEdges.size());
        }

        @Test
        @DisplayName("应正确获取入边")
        void getInEdges_shouldReturnCorrectEdges() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "expression"),
                    createNode("n2", "expression"),
                    createNode("n3", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n3"),
                    createEdge("e2", "n2", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            List<VisualEdge> inEdges = graph.getInEdges("n3");
            assertEquals(2, inEdges.size());
        }

        @Test
        @DisplayName("无出边节点应返回空列表")
        void getOutEdges_noEdges_shouldReturnEmptyList() {
            List<VisualNode> nodes = Collections.singletonList(
                    createNode("n1", "end")
            );

            FlowGraph graph = FlowGraph.build(nodes, Collections.emptyList());

            List<VisualEdge> outEdges = graph.getOutEdges("n1");
            assertTrue(outEdges.isEmpty());
        }

        @Test
        @DisplayName("应通过sourceHandle获取边")
        void getOutEdgeByHandle_shouldReturnCorrectEdge() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "if"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdgeWithHandle("e1", "n1", "n2", "then", null),
                    createEdgeWithHandle("e2", "n1", "n3", "else", null)
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            VisualEdge thenEdge = graph.getOutEdgeByHandle("n1", "then");
            assertNotNull(thenEdge);
            assertEquals("n2", thenEdge.getTarget());

            VisualEdge elseEdge = graph.getOutEdgeByHandle("n1", "else");
            assertNotNull(elseEdge);
            assertEquals("n3", elseEdge.getTarget());
        }
    }

    @Nested
    @DisplayName("节点邻接测试")
    class NodeAdjacencyTest {

        @Test
        @DisplayName("应正确获取后继节点")
        void getSuccessors_shouldReturnCorrectNodes() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n1", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            List<VisualNode> successors = graph.getSuccessors("n1");
            assertEquals(2, successors.size());
        }

        @Test
        @DisplayName("应正确获取前驱节点")
        void getPredecessors_shouldReturnCorrectNodes() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "expression"),
                    createNode("n2", "expression"),
                    createNode("n3", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n3"),
                    createEdge("e2", "n2", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            List<VisualNode> predecessors = graph.getPredecessors("n3");
            assertEquals(2, predecessors.size());
        }

        @Test
        @DisplayName("应通过handle获取后继节点")
        void getSuccessorByHandle_shouldReturnCorrectNode() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "if"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdgeWithHandle("e1", "n1", "n2", "then", null),
                    createEdgeWithHandle("e2", "n1", "n3", "else", null)
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            VisualNode thenNode = graph.getSuccessorByHandle("n1", "then");
            assertNotNull(thenNode);
            assertEquals("n2", thenNode.getId());
        }
    }

    @Nested
    @DisplayName("可达性测试")
    class ReachabilityTest {

        @Test
        @DisplayName("应正确找到可达节点")
        void findReachableNodes_shouldReturnAllReachable() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression"),
                    createNode("n4", "end"),
                    createNode("n5", "expression") // 不可达节点
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n2", "n3"),
                    createEdge("e3", "n3", "n4")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            Set<String> reachable = graph.findReachableNodes();
            assertEquals(4, reachable.size());
            assertTrue(reachable.contains("n1"));
            assertTrue(reachable.contains("n2"));
            assertTrue(reachable.contains("n3"));
            assertTrue(reachable.contains("n4"));
            assertFalse(reachable.contains("n5"));
        }

        @Test
        @DisplayName("无起始节点应返回空集")
        void findReachableNodes_noStart_shouldReturnEmpty() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "expression"),
                    createNode("n2", "end")
            );
            List<VisualEdge> edges = Collections.singletonList(
                    createEdge("e1", "n1", "n2")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            Set<String> reachable = graph.findReachableNodes();
            assertTrue(reachable.isEmpty());
        }
    }

    @Nested
    @DisplayName("拓扑排序测试")
    class TopologicalSortTest {

        @Test
        @DisplayName("简单图应正确排序")
        void topologicalSort_simpleGraph_shouldReturnCorrectOrder() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n2", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            List<VisualNode> sorted = graph.topologicalSort();
            assertEquals(3, sorted.size());
            // n1应在n2之前，n2应在n3之前
            assertTrue(indexOf(sorted, "n1") < indexOf(sorted, "n2"));
            assertTrue(indexOf(sorted, "n2") < indexOf(sorted, "n3"));
        }

        @Test
        @DisplayName("分支图应正确排序")
        void topologicalSort_branchGraph_shouldReturnValidOrder() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression"),
                    createNode("n4", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n1", "n3"),
                    createEdge("e3", "n2", "n4"),
                    createEdge("e4", "n3", "n4")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            List<VisualNode> sorted = graph.topologicalSort();
            assertEquals(4, sorted.size());
            // n1应在n2和n3之前
            assertTrue(indexOf(sorted, "n1") < indexOf(sorted, "n2"));
            assertTrue(indexOf(sorted, "n1") < indexOf(sorted, "n3"));
            // n2和n3应在n4之前
            assertTrue(indexOf(sorted, "n2") < indexOf(sorted, "n4"));
            assertTrue(indexOf(sorted, "n3") < indexOf(sorted, "n4"));
        }
    }

    @Nested
    @DisplayName("循环检测测试")
    class CycleDetectionTest {

        @Test
        @DisplayName("无循环图应返回false")
        void hasCycle_noCycle_shouldReturnFalse() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "end")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n2", "n3")
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            assertFalse(graph.hasCycle());
        }

        @Test
        @DisplayName("有循环图应返回true")
        void hasCycle_withCycle_shouldReturnTrue() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression"),
                    createNode("n3", "expression")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n2", "n3"),
                    createEdge("e3", "n3", "n2") // 形成循环
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            assertTrue(graph.hasCycle());
        }

        @Test
        @DisplayName("自循环应返回true")
        void hasCycle_selfLoop_shouldReturnTrue() {
            List<VisualNode> nodes = Arrays.asList(
                    createNode("n1", "start"),
                    createNode("n2", "expression")
            );
            List<VisualEdge> edges = Arrays.asList(
                    createEdge("e1", "n1", "n2"),
                    createEdge("e2", "n2", "n2") // 自循环
            );

            FlowGraph graph = FlowGraph.build(nodes, edges);

            assertTrue(graph.hasCycle());
        }
    }

    // ==================== 辅助方法 ====================

    private VisualNode createNode(String id, String type) {
        NodeData data;
        switch (type) {
            case "start":
                data = new StartNodeData();
                break;
            case "end":
                data = new EndNodeData();
                break;
            default:
                data = new ExpressionNodeData();
        }
        return VisualNode.builder()
                .id(id)
                .type(type)
                .data(data)
                .build();
    }

    private VisualEdge createEdge(String id, String source, String target) {
        return VisualEdge.builder()
                .id(id)
                .source(source)
                .target(target)
                .type(EdgeType.DEFAULT)
                .build();
    }

    private VisualEdge createEdgeWithHandle(String id, String source, String target,
                                            String sourceHandle, String targetHandle) {
        return VisualEdge.builder()
                .id(id)
                .source(source)
                .target(target)
                .sourceHandle(sourceHandle)
                .targetHandle(targetHandle)
                .type(EdgeType.DEFAULT)
                .build();
    }

    private int indexOf(List<VisualNode> list, String nodeId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(nodeId)) {
                return i;
            }
        }
        return -1;
    }
}

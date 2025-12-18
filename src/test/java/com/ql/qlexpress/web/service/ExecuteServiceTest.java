package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.model.enums.EdgeType;
import com.ql.qlexpress.web.model.visual.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecuteService 单元测试
 *
 * @author qlexpress
 */
class ExecuteServiceTest {

    private ExecuteService executeService;
    private TranspileService transpileService;

    @BeforeEach
    void setUp() {
        VisualToQLTranspiler transpiler = new VisualToQLTranspiler();
        transpileService = new TranspileService(transpiler);
        executeService = new ExecuteService(transpiler, transpileService);
    }

    @Nested
    @DisplayName("executeScript测试")
    class ExecuteScriptTest {

        @Test
        @DisplayName("简单表达式应正确执行")
        void executeScript_simpleExpression_shouldReturnCorrectResult() {
            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("1 + 2")
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals(3, response.getResult());
            assertNotNull(response.getStats());
        }

        @Test
        @DisplayName("带上下文变量的脚本应正确执行")
        void executeScript_withContext_shouldUseContextVariables() {
            Map<String, Object> context = new HashMap<>();
            context.put("a", 10);
            context.put("b", 20);

            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("a + b")
                    .context(context)
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals(30, response.getResult());
        }

        @Test
        @DisplayName("语法错误脚本应返回失败")
        void executeScript_syntaxError_shouldReturnFailure() {
            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("1 + +")
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getError());
        }

        @Test
        @DisplayName("复杂脚本应正确执行")
        void executeScript_complexScript_shouldExecuteCorrectly() {
            String script = "sum = 0;\n" +
                    "for (i = 1; i <= 10; i = i + 1) {\n" +
                    "    sum = sum + i;\n" +
                    "}\n" +
                    "return sum;";

            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script(script)
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals(55L, ((Number) response.getResult()).longValue());
        }

        @Test
        @DisplayName("带执行选项的脚本应正确执行")
        void executeScript_withOptions_shouldApplyOptions() {
            ExecuteOptions options = ExecuteOptions.builder()
                    .timeout(5000)
                    .enableCache(false)
                    .build();

            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("100 * 2")
                    .options(options)
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals(200, response.getResult());
        }

        @Test
        @DisplayName("返回null的脚本应正确处理")
        void executeScript_returnNull_shouldHandleCorrectly() {
            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("null")
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertNull(response.getResult());
        }

        @Test
        @DisplayName("字符串操作应正确执行")
        void executeScript_stringOperations_shouldExecuteCorrectly() {
            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("\"hello\" + \" \" + \"world\"")
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals("hello world", response.getResult());
        }

        @Test
        @DisplayName("布尔表达式应正确执行")
        void executeScript_booleanExpression_shouldExecuteCorrectly() {
            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("10 > 5 && 3 < 7")
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals(true, response.getResult());
        }
    }

    @Nested
    @DisplayName("executeFlow测试")
    class ExecuteFlowTest {

        @Test
        @DisplayName("简单流程应正确执行")
        void executeFlow_simpleFlow_shouldExecuteCorrectly() {
            VisualFlowSchema flow = createSimpleFlow();

            ExecuteFlowRequest request = ExecuteFlowRequest.builder()
                    .flow(flow)
                    .build();

            ExecuteFlowResponse response = executeService.executeFlow(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getGeneratedScript());
        }

        @Test
        @DisplayName("带上下文的流程应正确执行")
        void executeFlow_withContext_shouldExecuteCorrectly() {
            VisualFlowSchema flow = createFlowWithVariableReference();

            Map<String, Object> context = new HashMap<>();
            context.put("inputValue", 100);

            ExecuteFlowRequest request = ExecuteFlowRequest.builder()
                    .flow(flow)
                    .context(context)
                    .build();

            ExecuteFlowResponse response = executeService.executeFlow(request);

            assertTrue(response.isSuccess());
        }

        @Test
        @DisplayName("包含节点详情的流程应返回详细信息")
        void executeFlow_includeNodeDetails_shouldReturnDetails() {
            VisualFlowSchema flow = createSimpleFlow();

            ExecuteFlowRequest request = ExecuteFlowRequest.builder()
                    .flow(flow)
                    .includeNodeDetails(true)
                    .build();

            ExecuteFlowResponse response = executeService.executeFlow(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getNodeDetails());
            assertNotNull(response.getExecutionPath());
        }

        @Test
        @DisplayName("带条件分支的流程应正确执行")
        void executeFlow_withIfBranch_shouldExecuteCorrectly() {
            VisualFlowSchema flow = createIfBranchFlow();

            Map<String, Object> context = new HashMap<>();
            context.put("x", 10);

            ExecuteFlowRequest request = ExecuteFlowRequest.builder()
                    .flow(flow)
                    .context(context)
                    .build();

            ExecuteFlowResponse response = executeService.executeFlow(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getGeneratedScript());
            assertTrue(response.getGeneratedScript().contains("if"));
        }

        @Test
        @DisplayName("无效流程应返回转译错误")
        void executeFlow_invalidFlow_shouldReturnTranspileError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.emptyList())
                    .build();

            ExecuteFlowRequest request = ExecuteFlowRequest.builder()
                    .flow(flow)
                    .build();

            ExecuteFlowResponse response = executeService.executeFlow(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getError());
            assertEquals("TRANSPILE_ERROR", response.getError().getType());
        }
    }

    @Nested
    @DisplayName("统计信息测试")
    class StatsTest {

        @Test
        @DisplayName("执行结果应包含时间统计")
        void executeScript_shouldIncludeTimeStats() {
            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("1 + 1")
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getStats());
            assertTrue(response.getStats().getExecutionTimeMs() >= 0);
        }
    }

    @Nested
    @DisplayName("上下文变量测试")
    class ContextTest {

        @Test
        @DisplayName("执行后应返回更新的上下文")
        void executeScript_shouldReturnUpdatedContext() {
            Map<String, Object> context = new HashMap<>();

            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("result = 42")
                    .context(context)
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getOutputContext());
            // QL脚本中的赋值会更新上下文
        }

        @Test
        @DisplayName("列表元素访问应正确执行")
        void executeScript_listOperations_shouldExecuteCorrectly() {
            Map<String, Object> context = new HashMap<>();
            context.put("list", new ArrayList<>(Arrays.asList(1, 2, 3)));

            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("list[0]")
                    .context(context)
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals(1, response.getResult());
        }

        @Test
        @DisplayName("Map元素访问应正确执行")
        void executeScript_mapOperations_shouldExecuteCorrectly() {
            Map<String, Object> context = new HashMap<>();
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("key1", "value1");
            context.put("map", testMap);

            ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                    .script("map[\"key1\"]")
                    .context(context)
                    .build();

            ExecuteResponse response = executeService.executeScript(request);

            assertTrue(response.isSuccess());
            assertEquals("value1", response.getResult());
        }
    }

    // ==================== 辅助方法 ====================

    private VisualFlowSchema createSimpleFlow() {
        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result",
                                Expression.operator("+", Expression.literal(1), Expression.literal(2))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    private VisualFlowSchema createFlowWithVariableReference() {
        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result",
                                Expression.operator("*", Expression.variable("inputValue"), Expression.literal(2))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    private VisualFlowSchema createIfBranchFlow() {
        IfNodeData ifData = new IfNodeData();
        ifData.setLabel("条件判断");
        ifData.setCondition(Expression.operator(">", Expression.variable("x"), Expression.literal(0)));
        ifData.setThenBranch("then1");

        VisualNode ifNode = VisualNode.builder()
                .id("if1")
                .type("if")
                .data(ifData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        ifNode,
                        createExpressionNode("then1", "result", Expression.literal("positive")),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "if1"),
                        createEdgeWithHandle("e2", "if1", "then1", "then", null),
                        createEdge("e3", "then1", "end")
                ))
                .build();
    }

    private VisualNode createStartNode(String id) {
        StartNodeData data = new StartNodeData();
        data.setLabel("开始");
        return VisualNode.builder()
                .id(id)
                .type("start")
                .data(data)
                .build();
    }

    private VisualNode createEndNode(String id) {
        EndNodeData data = new EndNodeData();
        data.setLabel("结束");
        return VisualNode.builder()
                .id(id)
                .type("end")
                .data(data)
                .build();
    }

    private VisualNode createExpressionNode(String id, String resultVariable, Expression expression) {
        ExpressionNodeData data = new ExpressionNodeData();
        data.setLabel("表达式");
        data.setResultVariable(resultVariable);
        data.setExpression(expression);
        return VisualNode.builder()
                .id(id)
                .type("expression")
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
}

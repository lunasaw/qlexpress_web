package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.transpiler.TranspileOptions;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.exception.TranspileException;
import com.ql.qlexpress.web.model.dto.TranspileToQLResponse;
import com.ql.qlexpress.web.model.enums.EdgeType;
import com.ql.qlexpress.web.model.visual.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TranspileService 单元测试
 *
 * @author qlexpress
 */
class TranspileServiceTest {

    private TranspileService transpileService;

    @BeforeEach
    void setUp() {
        VisualToQLTranspiler transpiler = new VisualToQLTranspiler();
        transpileService = new TranspileService(transpiler);
    }

    @Nested
    @DisplayName("transpileToQL测试")
    class TranspileToQLTest {

        @Test
        @DisplayName("简单流程应正确转译")
        void transpileToQL_simpleFlow_shouldReturnCorrectResponse() {
            VisualFlowSchema schema = createSimpleFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertNotNull(response.getScript());
            assertTrue(response.getScript().contains("result = (1 + 2);"));
            assertNotNull(response.getStats());
            assertTrue(response.getStats().getTranspileTimeMs() >= 0);
        }

        @Test
        @DisplayName("带选项的转译应正确应用选项")
        void transpileToQL_withOptions_shouldApplyOptions() {
            VisualFlowSchema schema = createSimpleFlow();
            TranspileOptions options = TranspileOptions.builder()
                    .format(true)
                    .generateComments(true)
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema, options);

            assertNotNull(response);
            assertTrue(response.getScript().contains("\n"));
        }

        @Test
        @DisplayName("无效流程应抛出异常")
        void transpileToQL_invalidFlow_shouldThrowException() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.emptyList())
                    .build();

            assertThrows(TranspileException.class, () ->
                    transpileService.transpileToQL(schema));
        }

        @Test
        @DisplayName("空选项应使用默认值")
        void transpileToQL_nullOptions_shouldUseDefaults() {
            VisualFlowSchema schema = createSimpleFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema, null);

            assertNotNull(response);
            assertNotNull(response.getScript());
        }
    }

    @Nested
    @DisplayName("quickTranspile测试")
    class QuickTranspileTest {

        @Test
        @DisplayName("快速转译应仅返回脚本")
        void quickTranspile_shouldReturnScriptOnly() {
            VisualFlowSchema schema = createSimpleFlow();

            String script = transpileService.quickTranspile(schema);

            assertNotNull(script);
            assertTrue(script.contains("result = (1 + 2);"));
        }
    }

    @Nested
    @DisplayName("transpileWithFormat测试")
    class TranspileWithFormatTest {

        @Test
        @DisplayName("格式化转译应生成注释")
        void transpileWithFormat_shouldIncludeComments() {
            VisualFlowSchema schema = createFlowWithVariables();

            String script = transpileService.transpileWithFormat(schema);

            assertNotNull(script);
            assertTrue(script.contains("// 变量声明"));
        }
    }

    @Nested
    @DisplayName("transpileCompact测试")
    class TranspileCompactTest {

        @Test
        @DisplayName("紧凑转译应无换行")
        void transpileCompact_shouldHaveNoNewlines() {
            VisualFlowSchema schema = createMultiStatementFlow();

            String script = transpileService.transpileCompact(schema);

            assertNotNull(script);
            assertFalse(script.contains("\n"));
        }
    }

    @Nested
    @DisplayName("复杂流程测试")
    class ComplexFlowTest {

        @Test
        @DisplayName("带条件分支的流程应正确转译")
        void transpileToQL_withIfBranch_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createIfBranchFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            assertTrue(script.contains("if ((x > 0)) {"));
        }

        @Test
        @DisplayName("带循环的流程应正确转译")
        void transpileToQL_withLoop_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createForLoopFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            assertTrue(script.contains("for ("));
            assertTrue(script.contains("}"));
        }

        @Test
        @DisplayName("带函数调用的流程应正确转译")
        void transpileToQL_withFunctionCall_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createFunctionCallFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            assertTrue(script.contains("max(1, 2, 3)"));
        }
    }

    @Nested
    @DisplayName("源映射测试")
    class SourceMappingTest {

        @Test
        @DisplayName("转译结果应包含源映射")
        void transpileToQL_shouldIncludeSourceMapping() {
            VisualFlowSchema schema = createSimpleFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response.getSourceMap());
            assertFalse(response.getSourceMap().isEmpty());
        }
    }

    @Nested
    @DisplayName("统计信息测试")
    class StatsTest {

        @Test
        @DisplayName("转译结果应包含正确的统计信息")
        void transpileToQL_shouldIncludeCorrectStats() {
            VisualFlowSchema schema = createSimpleFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response.getStats());
            assertEquals(3, response.getStats().getNodeCount());
            assertEquals(2, response.getStats().getEdgeCount());
            assertTrue(response.getStats().getLineCount() > 0);
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

    private VisualFlowSchema createMultiStatementFlow() {
        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createAssignmentNode("assign1", "a", Expression.literal(1)),
                        createAssignmentNode("assign2", "b", Expression.literal(2)),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "assign1"),
                        createEdge("e2", "assign1", "assign2"),
                        createEdge("e3", "assign2", "end")
                ))
                .build();
    }

    private VisualFlowSchema createFlowWithVariables() {
        return VisualFlowSchema.builder()
                .version("1.0.0")
                .variables(Collections.singletonList(
                        VariableDefinition.builder()
                                .name("count")
                                .type("int")
                                .initialValue(Expression.literal(0))
                                .build()
                ))
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createEndNode("end")
                ))
                .edges(Collections.singletonList(
                        createEdge("e1", "start", "end")
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

    private VisualFlowSchema createForLoopFlow() {
        ForNodeData forData = new ForNodeData();
        forData.setLabel("循环");
        forData.setInit(Expression.raw("i = 0"));
        forData.setCondition(Expression.operator("<", Expression.variable("i"), Expression.literal(10)));
        forData.setUpdate(Expression.raw("i = i + 1"));
        forData.setBodyEntrance("body1");

        VisualNode forNode = VisualNode.builder()
                .id("for1")
                .type("for")
                .data(forData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        forNode,
                        createExpressionNode("body1", "sum",
                                Expression.operator("+", Expression.variable("sum"), Expression.variable("i"))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "for1"),
                        createEdgeWithHandle("e2", "for1", "body1", "body", null),
                        createEdgeWithHandle("e3", "for1", "end", "next", null)
                ))
                .build();
    }

    private VisualFlowSchema createFunctionCallFlow() {
        FunctionCallNodeData funcData = new FunctionCallNodeData();
        funcData.setLabel("函数调用");
        funcData.setFunctionName("max");
        funcData.setArguments(Arrays.asList(
                Expression.literal(1),
                Expression.literal(2),
                Expression.literal(3)
        ));
        funcData.setResultVariable("maxVal");

        VisualNode funcNode = VisualNode.builder()
                .id("func1")
                .type("function_call")
                .data(funcData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        funcNode,
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "func1"),
                        createEdge("e2", "func1", "end")
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

    private VisualNode createAssignmentNode(String id, String variable, Expression value) {
        AssignmentNodeData data = new AssignmentNodeData();
        data.setLabel("赋值");
        data.setVariable(variable);
        data.setValue(value);
        return VisualNode.builder()
                .id(id)
                .type("assignment")
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

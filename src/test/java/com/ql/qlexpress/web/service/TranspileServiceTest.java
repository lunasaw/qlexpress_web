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

    @Nested
    @DisplayName("复杂嵌套结构测试")
    class NestedStructureTest {

        @Test
        @DisplayName("嵌套IF-ELSE结构应正确转译")
        void transpileToQL_nestedIfElse_shouldGenerateCorrectQL() {
            // 创建嵌套的if-else结构：if (x > 0) { if (x > 10) { ... } else { ... } } else { ... }
            VisualFlowSchema schema = createNestedIfElseFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证外层if
            assertTrue(script.contains("if ((x > 0)) {"));
            // 验证嵌套的内层if
            assertTrue(script.contains("if ((x > 10)) {"));
            // 验证else分支
            assertTrue(script.contains("} else {"));
        }

        @Test
        @DisplayName("循环内嵌套IF应正确转译")
        void transpileToQL_loopWithNestedIf_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createLoopWithNestedIfFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证for循环
            assertTrue(script.contains("for ("));
            // 验证循环内的if
            assertTrue(script.contains("if ("));
        }

        @Test
        @DisplayName("多层嵌套循环应正确转译")
        void transpileToQL_nestedLoops_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createNestedLoopsFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证外层for循环
            assertTrue(script.contains("for (i = 0; (i < 5); i = i + 1) {"));
            // 验证内层for循环
            assertTrue(script.contains("for (j = 0; (j < 3); j = j + 1) {"));
        }

        @Test
        @DisplayName("IF内嵌套循环应正确转译")
        void transpileToQL_ifWithNestedLoop_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createIfWithNestedLoopFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            assertTrue(script.contains("if ("));
            assertTrue(script.contains("for (") || script.contains("while ("));
        }
    }

    @Nested
    @DisplayName("复杂表达式测试")
    class ComplexExpressionTest {

        @Test
        @DisplayName("多操作符表达式应正确转译")
        void transpileToQL_multiOperatorExpression_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createMultiOperatorExpressionFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证复杂表达式 (a + b) * (c - d)
            assertTrue(script.contains("((a + b) * (c - d))"));
        }

        @Test
        @DisplayName("嵌套函数调用表达式应正确转译")
        void transpileToQL_nestedFunctionCalls_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createNestedFunctionCallFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证嵌套函数调用 max(min(a, b), c)
            assertTrue(script.contains("max(min(a, b), c)"));
        }

        @Test
        @DisplayName("链式方法调用应正确转译")
        void transpileToQL_chainedMethodCalls_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createChainedMethodCallFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证链式方法调用 str.trim().toLowerCase()
            assertTrue(script.contains(".trim()") && script.contains(".toLowerCase()"));
        }

        @Test
        @DisplayName("复杂逻辑表达式应正确转译")
        void transpileToQL_complexLogicalExpression_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createComplexLogicalExpressionFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证复杂逻辑表达式 (a && b) || (c && d)
            assertTrue(script.contains("&&") && script.contains("||"));
        }

        @Test
        @DisplayName("三元运算符表达式应正确转译")
        void transpileToQL_ternaryOperator_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createTernaryOperatorFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证三元运算符
            assertTrue(script.contains("?") && script.contains(":"));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionTest {

        @Test
        @DisplayName("单节点流程应正确处理")
        void transpileToQL_singleNodeFlow_shouldHandleCorrectly() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createExpressionNode("expr1", "result", Expression.literal(42)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "expr1"),
                            createEdge("e2", "expr1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains("result = 42;"));
        }

        @Test
        @DisplayName("空字符串字面量应正确转译")
        void transpileToQL_emptyStringLiteral_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "str", Expression.literal("")),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains("str = \"\";"));
        }

        @Test
        @DisplayName("特殊字符字符串应正确转译")
        void transpileToQL_specialCharacterString_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "str", Expression.literal("hello\\nworld")),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertNotNull(response.getScript());
        }

        @Test
        @DisplayName("null值应正确转译")
        void transpileToQL_nullValue_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "obj", Expression.literal(null)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains("null"));
        }

        @Test
        @DisplayName("超长变量名应正确转译")
        void transpileToQL_longVariableName_shouldGenerateCorrectQL() {
            String longVarName = "thisIsAVeryLongVariableNameThatShouldStillWorkCorrectly";
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", longVarName, Expression.literal(1)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains(longVarName));
        }

        @Test
        @DisplayName("负数应正确转译")
        void transpileToQL_negativeNumber_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "num", Expression.literal(-42)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains("-42"));
        }

        @Test
        @DisplayName("浮点数应正确转译")
        void transpileToQL_floatNumber_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "num", Expression.literal(3.14159)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains("3.14159"));
        }

        @Test
        @DisplayName("布尔值应正确转译")
        void transpileToQL_booleanValue_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "flag1", Expression.literal(true)),
                            createAssignmentNode("assign2", "flag2", Expression.literal(false)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "assign2"),
                            createEdge("e3", "assign2", "end")
                    ))
                    .build();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            assertTrue(response.getScript().contains("true") && response.getScript().contains("false"));
        }
    }

    @Nested
    @DisplayName("完整业务流程测试")
    class BusinessFlowTest {

        @Test
        @DisplayName("计算折扣业务流程应正确转译")
        void transpileToQL_discountCalculation_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createDiscountCalculationFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证业务逻辑相关的关键字
            assertTrue(script.contains("discount"));
            assertTrue(script.contains("price") || script.contains("amount"));
        }

        @Test
        @DisplayName("订单状态流转业务流程应正确转译")
        void transpileToQL_orderStatusFlow_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createOrderStatusFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证状态判断
            assertTrue(script.contains("if (") || script.contains("status"));
        }

        @Test
        @DisplayName("批量数据处理业务流程应正确转译")
        void transpileToQL_batchDataProcessing_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createBatchDataProcessingFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证循环和数据处理
            assertTrue(script.contains("for (") || script.contains("while ("));
        }

        @Test
        @DisplayName("异常处理业务流程应正确转译")
        void transpileToQL_exceptionHandling_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createExceptionHandlingFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证try-catch结构
            assertTrue(script.contains("try {"));
            assertTrue(script.contains("catch ("));
        }

        @Test
        @DisplayName("数据验证业务流程应正确转译")
        void transpileToQL_dataValidation_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createDataValidationFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证多重条件验证
            assertTrue(script.contains("if ("));
        }
    }

    @Nested
    @DisplayName("混合场景测试")
    class MixedScenarioTest {

        @Test
        @DisplayName("带变量声明和import的完整流程应正确转译")
        void transpileToQL_fullFlowWithVarsAndImports_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createFullFlowWithVarsAndImports();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证import语句
            assertTrue(script.contains("import java.util."));
            // 验证变量声明
            assertTrue(script.contains("int ") || script.contains("String "));
        }

        @Test
        @DisplayName("多分支合并流程应正确转译")
        void transpileToQL_multiBranchMerge_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createMultiBranchMergeFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证多个if分支
            assertTrue(script.contains("if ("));
        }

        @Test
        @DisplayName("带早期返回的流程应正确转译")
        void transpileToQL_earlyReturn_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createEarlyReturnFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证return语句
            assertTrue(script.contains("return"));
        }

        @Test
        @DisplayName("循环内带break和continue应正确转译")
        void transpileToQL_loopWithBreakContinue_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = createLoopWithBreakContinueFlow();

            TranspileToQLResponse response = transpileService.transpileToQL(schema);

            assertNotNull(response);
            String script = response.getScript();
            // 验证break或continue
            assertTrue(script.contains("break;") || script.contains("continue;"));
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

    // ==================== 复杂嵌套结构辅助方法 ====================

    /**
     * 创建嵌套IF-ELSE流程
     * if (x > 0) {
     *     if (x > 10) {
     *         result = "large";
     *     } else {
     *         result = "small";
     *     }
     * } else {
     *     result = "negative";
     * }
     */
    private VisualFlowSchema createNestedIfElseFlow() {
        // 外层if节点
        IfNodeData outerIfData = new IfNodeData();
        outerIfData.setLabel("外层条件");
        outerIfData.setCondition(Expression.operator(">", Expression.variable("x"), Expression.literal(0)));
        outerIfData.setThenBranch("innerIf");
        outerIfData.setElseBranch("elseExpr");

        VisualNode outerIfNode = VisualNode.builder()
                .id("outerIf")
                .type("if")
                .data(outerIfData)
                .build();

        // 内层if节点
        IfNodeData innerIfData = new IfNodeData();
        innerIfData.setLabel("内层条件");
        innerIfData.setCondition(Expression.operator(">", Expression.variable("x"), Expression.literal(10)));
        innerIfData.setThenBranch("thenExpr");
        innerIfData.setElseBranch("smallExpr");

        VisualNode innerIfNode = VisualNode.builder()
                .id("innerIf")
                .type("if")
                .data(innerIfData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        outerIfNode,
                        innerIfNode,
                        createExpressionNode("thenExpr", "result", Expression.literal("large")),
                        createExpressionNode("smallExpr", "result", Expression.literal("small")),
                        createExpressionNode("elseExpr", "result", Expression.literal("negative")),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "outerIf"),
                        createEdgeWithHandle("e2", "outerIf", "innerIf", "then", null),
                        createEdgeWithHandle("e3", "outerIf", "elseExpr", "else", null),
                        createEdgeWithHandle("e4", "innerIf", "thenExpr", "then", null),
                        createEdgeWithHandle("e5", "innerIf", "smallExpr", "else", null),
                        createEdge("e6", "thenExpr", "end"),
                        createEdge("e7", "smallExpr", "end"),
                        createEdge("e8", "elseExpr", "end"),
                        createEdgeWithHandle("e9", "outerIf", "end", "next", null)
                ))
                .build();
    }

    /**
     * 创��循环内嵌套IF流程
     * for (i = 0; i < 10; i++) {
     *     if (i % 2 == 0) {
     *         sum = sum + i;
     *     }
     * }
     */
    private VisualFlowSchema createLoopWithNestedIfFlow() {
        // for节点
        ForNodeData forData = new ForNodeData();
        forData.setLabel("循环");
        forData.setInit(Expression.raw("i = 0"));
        forData.setCondition(Expression.operator("<", Expression.variable("i"), Expression.literal(10)));
        forData.setUpdate(Expression.raw("i = i + 1"));
        forData.setBodyEntrance("bodyIf");

        VisualNode forNode = VisualNode.builder()
                .id("for1")
                .type("for")
                .data(forData)
                .build();

        // 循环体内的if节点
        IfNodeData ifData = new IfNodeData();
        ifData.setLabel("条件判断");
        ifData.setCondition(Expression.operator("==",
                Expression.operator("%", Expression.variable("i"), Expression.literal(2)),
                Expression.literal(0)));
        ifData.setThenBranch("addExpr");

        VisualNode ifNode = VisualNode.builder()
                .id("bodyIf")
                .type("if")
                .data(ifData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        forNode,
                        ifNode,
                        createExpressionNode("addExpr", "sum",
                                Expression.operator("+", Expression.variable("sum"), Expression.variable("i"))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "for1"),
                        createEdgeWithHandle("e2", "for1", "bodyIf", "body", null),
                        createEdgeWithHandle("e3", "bodyIf", "addExpr", "then", null),
                        createEdgeWithHandle("e4", "for1", "end", "next", null)
                ))
                .build();
    }

    /**
     * 创建多层嵌套循环流程
     * for (i = 0; i < 5; i++) {
     *     for (j = 0; j < 3; j++) {
     *         result = result + i * j;
     *     }
     * }
     */
    private VisualFlowSchema createNestedLoopsFlow() {
        // 外层for节点
        ForNodeData outerForData = new ForNodeData();
        outerForData.setLabel("外层循环");
        outerForData.setInit(Expression.raw("i = 0"));
        outerForData.setCondition(Expression.operator("<", Expression.variable("i"), Expression.literal(5)));
        outerForData.setUpdate(Expression.raw("i = i + 1"));
        outerForData.setBodyEntrance("innerFor");

        VisualNode outerForNode = VisualNode.builder()
                .id("outerFor")
                .type("for")
                .data(outerForData)
                .build();

        // 内层for节点
        ForNodeData innerForData = new ForNodeData();
        innerForData.setLabel("内层循环");
        innerForData.setInit(Expression.raw("j = 0"));
        innerForData.setCondition(Expression.operator("<", Expression.variable("j"), Expression.literal(3)));
        innerForData.setUpdate(Expression.raw("j = j + 1"));
        innerForData.setBodyEntrance("bodyExpr");

        VisualNode innerForNode = VisualNode.builder()
                .id("innerFor")
                .type("for")
                .data(innerForData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        outerForNode,
                        innerForNode,
                        createExpressionNode("bodyExpr", "result",
                                Expression.operator("+", Expression.variable("result"),
                                        Expression.operator("*", Expression.variable("i"), Expression.variable("j")))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "outerFor"),
                        createEdgeWithHandle("e2", "outerFor", "innerFor", "body", null),
                        createEdgeWithHandle("e3", "innerFor", "bodyExpr", "body", null),
                        createEdgeWithHandle("e4", "outerFor", "end", "next", null)
                ))
                .build();
    }

    /**
     * 创建IF内嵌套循环流程
     */
    private VisualFlowSchema createIfWithNestedLoopFlow() {
        // if节点
        IfNodeData ifData = new IfNodeData();
        ifData.setLabel("条件判断");
        ifData.setCondition(Expression.operator(">", Expression.variable("count"), Expression.literal(0)));
        ifData.setThenBranch("bodyFor");

        VisualNode ifNode = VisualNode.builder()
                .id("if1")
                .type("if")
                .data(ifData)
                .build();

        // if分支内的for节点
        ForNodeData forData = new ForNodeData();
        forData.setLabel("循环");
        forData.setInit(Expression.raw("i = 0"));
        forData.setCondition(Expression.operator("<", Expression.variable("i"), Expression.variable("count")));
        forData.setUpdate(Expression.raw("i = i + 1"));
        forData.setBodyEntrance("loopBody");

        VisualNode forNode = VisualNode.builder()
                .id("bodyFor")
                .type("for")
                .data(forData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        ifNode,
                        forNode,
                        createExpressionNode("loopBody", "sum",
                                Expression.operator("+", Expression.variable("sum"), Expression.variable("i"))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "if1"),
                        createEdgeWithHandle("e2", "if1", "bodyFor", "then", null),
                        createEdgeWithHandle("e3", "bodyFor", "loopBody", "body", null),
                        createEdgeWithHandle("e4", "if1", "end", "next", null)
                ))
                .build();
    }

    // ==================== 复杂表达式辅助方法 ====================

    /**
     * 创建多操作符表达式流程
     * result = (a + b) * (c - d)
     */
    private VisualFlowSchema createMultiOperatorExpressionFlow() {
        Expression addExpr = Expression.operator("+", Expression.variable("a"), Expression.variable("b"));
        Expression subExpr = Expression.operator("-", Expression.variable("c"), Expression.variable("d"));
        Expression mulExpr = Expression.operator("*", addExpr, subExpr);

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result", mulExpr),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    /**
     * 创建嵌套函数调用流程
     * result = max(min(a, b), c)
     */
    private VisualFlowSchema createNestedFunctionCallFlow() {
        Expression minExpr = Expression.function("min", Arrays.asList(
                Expression.variable("a"), Expression.variable("b")));
        Expression maxExpr = Expression.function("max", Arrays.asList(minExpr, Expression.variable("c")));

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result", maxExpr),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    /**
     * 创建链式方法调用流程
     * result = str.trim().toLowerCase()
     */
    private VisualFlowSchema createChainedMethodCallFlow() {
        Expression trimExpr = Expression.method(Expression.variable("str"), "trim", Collections.emptyList());
        Expression lowerExpr = Expression.method(trimExpr, "toLowerCase", Collections.emptyList());

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result", lowerExpr),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    /**
     * 创建复杂逻辑表达式流程
     * result = (a && b) || (c && d)
     */
    private VisualFlowSchema createComplexLogicalExpressionFlow() {
        Expression and1 = Expression.operator("&&", Expression.variable("a"), Expression.variable("b"));
        Expression and2 = Expression.operator("&&", Expression.variable("c"), Expression.variable("d"));
        Expression orExpr = Expression.operator("||", and1, and2);

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result", orExpr),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    /**
     * 创建三元运算符流程
     * result = x > 0 ? "positive" : "non-positive"
     */
    private VisualFlowSchema createTernaryOperatorFlow() {
        // 使用raw表达式来表示三元运算符
        Expression ternaryExpr = Expression.raw("(x > 0) ? \"positive\" : \"non-positive\"");

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "result", ternaryExpr),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    // ==================== 业务流程辅助方法 ====================

    /**
     * 创建折扣计算业务流程
     * if (amount >= 1000) {
     *     discount = 0.2;
     * } else if (amount >= 500) {
     *     discount = 0.1;
     * } else {
     *     discount = 0;
     * }
     * finalPrice = amount * (1 - discount);
     */
    private VisualFlowSchema createDiscountCalculationFlow() {
        // 第一个if节点
        IfNodeData ifData1 = new IfNodeData();
        ifData1.setLabel("大额判断");
        ifData1.setCondition(Expression.operator(">=", Expression.variable("amount"), Expression.literal(1000)));
        ifData1.setThenBranch("discount20");
        ifData1.setElseBranch("if2");

        VisualNode ifNode1 = VisualNode.builder()
                .id("if1")
                .type("if")
                .data(ifData1)
                .build();

        // 第二个if节点
        IfNodeData ifData2 = new IfNodeData();
        ifData2.setLabel("中额判断");
        ifData2.setCondition(Expression.operator(">=", Expression.variable("amount"), Expression.literal(500)));
        ifData2.setThenBranch("discount10");
        ifData2.setElseBranch("discount0");

        VisualNode ifNode2 = VisualNode.builder()
                .id("if2")
                .type("if")
                .data(ifData2)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        ifNode1,
                        ifNode2,
                        createAssignmentNode("discount20", "discount", Expression.literal(0.2)),
                        createAssignmentNode("discount10", "discount", Expression.literal(0.1)),
                        createAssignmentNode("discount0", "discount", Expression.literal(0)),
                        createExpressionNode("finalCalc", "finalPrice",
                                Expression.operator("*", Expression.variable("amount"),
                                        Expression.operator("-", Expression.literal(1), Expression.variable("discount")))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "if1"),
                        createEdgeWithHandle("e2", "if1", "discount20", "then", null),
                        createEdgeWithHandle("e3", "if1", "if2", "else", null),
                        createEdgeWithHandle("e4", "if2", "discount10", "then", null),
                        createEdgeWithHandle("e5", "if2", "discount0", "else", null),
                        createEdge("e6", "discount20", "finalCalc"),
                        createEdge("e7", "discount10", "finalCalc"),
                        createEdge("e8", "discount0", "finalCalc"),
                        createEdge("e9", "finalCalc", "end")
                ))
                .build();
    }

    /**
     * 创建订单状态流转流程
     */
    private VisualFlowSchema createOrderStatusFlow() {
        IfNodeData ifData = new IfNodeData();
        ifData.setLabel("状态判断");
        ifData.setCondition(Expression.operator("==", Expression.variable("status"), Expression.literal("PENDING")));
        ifData.setThenBranch("processPending");
        ifData.setElseBranch("processOther");

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
                        createAssignmentNode("processPending", "status", Expression.literal("PROCESSING")),
                        createAssignmentNode("processOther", "result", Expression.literal("NO_ACTION")),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "if1"),
                        createEdgeWithHandle("e2", "if1", "processPending", "then", null),
                        createEdgeWithHandle("e3", "if1", "processOther", "else", null),
                        createEdge("e4", "processPending", "end"),
                        createEdge("e5", "processOther", "end")
                ))
                .build();
    }

    /**
     * 创建批量数据处理流程
     */
    private VisualFlowSchema createBatchDataProcessingFlow() {
        ForeachNodeData foreachData = new ForeachNodeData();
        foreachData.setLabel("遍历数据");
        foreachData.setIteratorVariable("item");
        foreachData.setIterable(Expression.variable("dataList"));
        foreachData.setBodyBranch("processItem");

        VisualNode foreachNode = VisualNode.builder()
                .id("foreach1")
                .type("foreach")
                .data(foreachData)
                .build();

        FunctionCallNodeData funcData = new FunctionCallNodeData();
        funcData.setLabel("处理数据项");
        funcData.setFunctionName("processItem");
        funcData.setArguments(Collections.singletonList(Expression.variable("item")));
        funcData.setResultVariable("processedItem");

        VisualNode funcNode = VisualNode.builder()
                .id("processItem")
                .type("function_call")
                .data(funcData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        foreachNode,
                        funcNode,
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "foreach1"),
                        createEdgeWithHandle("e2", "foreach1", "processItem", "body", null),
                        createEdgeWithHandle("e3", "foreach1", "end", "next", null)
                ))
                .build();
    }

    /**
     * 创建异常处理流程
     */
    private VisualFlowSchema createExceptionHandlingFlow() {
        // try块中的节点
        VisualNode tryExpr = createExpressionNode("tryExpr", "result",
                Expression.operator("/", Expression.variable("a"), Expression.variable("b")));

        // catch块中的节点
        VisualNode catchExpr = createAssignmentNode("catchExpr", "result", Expression.literal(-1));

        // try_catch节点
        TryCatchNodeData tryCatchData = new TryCatchNodeData();
        tryCatchData.setLabel("异常处理");
        tryCatchData.setTryBranch("tryExpr");

        TryCatchNodeData.CatchHandler handler = new TryCatchNodeData.CatchHandler();
        handler.setExceptionType("ArithmeticException");
        handler.setExceptionVariable("e");
        handler.setCatchBranch("catchExpr");
        tryCatchData.setCatchHandlers(Collections.singletonList(handler));

        VisualNode tryCatchNode = VisualNode.builder()
                .id("tryCatch1")
                .type("try_catch")
                .data(tryCatchData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        tryCatchNode,
                        tryExpr,
                        catchExpr,
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "tryCatch1"),
                        createEdgeWithHandle("e2", "tryCatch1", "tryExpr", "try", null),
                        createEdgeWithHandle("e3", "tryCatch1", "catchExpr", "catch", null),
                        createEdgeWithHandle("e4", "tryCatch1", "end", "next", null)
                ))
                .build();
    }

    /**
     * 创建数据验证流程
     */
    private VisualFlowSchema createDataValidationFlow() {
        // 第一层验证：非空检查
        IfNodeData nullCheckData = new IfNodeData();
        nullCheckData.setLabel("非空检查");
        nullCheckData.setCondition(Expression.operator("==", Expression.variable("input"), Expression.literal(null)));
        nullCheckData.setThenBranch("returnError1");
        nullCheckData.setElseBranch("lengthCheck");

        VisualNode nullCheckNode = VisualNode.builder()
                .id("nullCheck")
                .type("if")
                .data(nullCheckData)
                .build();

        // 第二层验证：长度检查
        IfNodeData lengthCheckData = new IfNodeData();
        lengthCheckData.setLabel("长度检查");
        lengthCheckData.setCondition(Expression.operator("<",
                Expression.method(Expression.variable("input"), "length", Collections.emptyList()),
                Expression.literal(3)));
        lengthCheckData.setThenBranch("returnError2");
        lengthCheckData.setElseBranch("returnSuccess");

        VisualNode lengthCheckNode = VisualNode.builder()
                .id("lengthCheck")
                .type("if")
                .data(lengthCheckData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        nullCheckNode,
                        lengthCheckNode,
                        createAssignmentNode("returnError1", "result", Expression.literal("ERROR_NULL")),
                        createAssignmentNode("returnError2", "result", Expression.literal("ERROR_TOO_SHORT")),
                        createAssignmentNode("returnSuccess", "result", Expression.literal("SUCCESS")),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "nullCheck"),
                        createEdgeWithHandle("e2", "nullCheck", "returnError1", "then", null),
                        createEdgeWithHandle("e3", "nullCheck", "lengthCheck", "else", null),
                        createEdgeWithHandle("e4", "lengthCheck", "returnError2", "then", null),
                        createEdgeWithHandle("e5", "lengthCheck", "returnSuccess", "else", null),
                        createEdge("e6", "returnError1", "end"),
                        createEdge("e7", "returnError2", "end"),
                        createEdge("e8", "returnSuccess", "end")
                ))
                .build();
    }

    // ==================== 混合场景辅助方法 ====================

    /**
     * 创建带变量声明和import的完整流程
     */
    private VisualFlowSchema createFullFlowWithVarsAndImports() {
        return VisualFlowSchema.builder()
                .version("1.0.0")
                .imports(Arrays.asList("java.util.List", "java.util.ArrayList"))
                .variables(Arrays.asList(
                        VariableDefinition.builder()
                                .name("count")
                                .type("int")
                                .initialValue(Expression.literal(0))
                                .build(),
                        VariableDefinition.builder()
                                .name("message")
                                .type("String")
                                .initialValue(Expression.literal("Hello"))
                                .build()
                ))
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1", "count",
                                Expression.operator("+", Expression.variable("count"), Expression.literal(1))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
                ))
                .build();
    }

    /**
     * 创建多分支合并流程
     */
    private VisualFlowSchema createMultiBranchMergeFlow() {
        IfNodeData ifData = new IfNodeData();
        ifData.setLabel("分支判断");
        ifData.setCondition(Expression.operator(">", Expression.variable("x"), Expression.literal(0)));
        ifData.setThenBranch("branch1");
        ifData.setElseBranch("branch2");

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
                        createAssignmentNode("branch1", "result", Expression.literal(1)),
                        createAssignmentNode("branch2", "result", Expression.literal(2)),
                        createExpressionNode("merge", "finalResult",
                                Expression.operator("*", Expression.variable("result"), Expression.literal(10))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "if1"),
                        createEdgeWithHandle("e2", "if1", "branch1", "then", null),
                        createEdgeWithHandle("e3", "if1", "branch2", "else", null),
                        createEdge("e4", "branch1", "merge"),
                        createEdge("e5", "branch2", "merge"),
                        createEdge("e6", "merge", "end")
                ))
                .build();
    }

    /**
     * 创建带早期返回的流程
     */
    private VisualFlowSchema createEarlyReturnFlow() {
        IfNodeData ifData = new IfNodeData();
        ifData.setLabel("前置检查");
        ifData.setCondition(Expression.operator("==", Expression.variable("input"), Expression.literal(null)));
        ifData.setThenBranch("earlyReturn");
        ifData.setElseBranch("normalProcess");

        VisualNode ifNode = VisualNode.builder()
                .id("if1")
                .type("if")
                .data(ifData)
                .build();

        ReturnNodeData returnData = new ReturnNodeData();
        returnData.setLabel("早期返回");
        returnData.setValue(Expression.literal(-1));

        VisualNode returnNode = VisualNode.builder()
                .id("earlyReturn")
                .type("return")
                .data(returnData)
                .build();

        ReturnNodeData normalReturnData = new ReturnNodeData();
        normalReturnData.setLabel("正常返回");
        normalReturnData.setValue(Expression.variable("result"));

        VisualNode normalReturnNode = VisualNode.builder()
                .id("normalReturn")
                .type("return")
                .data(normalReturnData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        ifNode,
                        returnNode,
                        createExpressionNode("normalProcess", "result",
                                Expression.operator("*", Expression.variable("input"), Expression.literal(2))),
                        normalReturnNode,
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "if1"),
                        createEdgeWithHandle("e2", "if1", "earlyReturn", "then", null),
                        createEdgeWithHandle("e3", "if1", "normalProcess", "else", null),
                        createEdge("e4", "earlyReturn", "end"),
                        createEdge("e5", "normalProcess", "normalReturn"),
                        createEdge("e6", "normalReturn", "end")
                ))
                .build();
    }

    /**
     * 创建循环内带break和continue的流程
     */
    private VisualFlowSchema createLoopWithBreakContinueFlow() {
        // for节点
        ForNodeData forData = new ForNodeData();
        forData.setLabel("循环");
        forData.setInit(Expression.raw("i = 0"));
        forData.setCondition(Expression.operator("<", Expression.variable("i"), Expression.literal(100)));
        forData.setUpdate(Expression.raw("i = i + 1"));
        forData.setBodyEntrance("checkBreak");

        VisualNode forNode = VisualNode.builder()
                .id("for1")
                .type("for")
                .data(forData)
                .build();

        // 检查是否需要break
        IfNodeData breakCheckData = new IfNodeData();
        breakCheckData.setLabel("检查中断");
        breakCheckData.setCondition(Expression.operator(">", Expression.variable("sum"), Expression.literal(1000)));
        breakCheckData.setThenBranch("breakNode");
        breakCheckData.setElseBranch("checkContinue");

        VisualNode breakCheckNode = VisualNode.builder()
                .id("checkBreak")
                .type("if")
                .data(breakCheckData)
                .build();

        // break节点
        BreakNodeData breakData = new BreakNodeData();
        breakData.setLabel("中断");

        VisualNode breakNode = VisualNode.builder()
                .id("breakNode")
                .type("break")
                .data(breakData)
                .build();

        // 检查是否需要continue
        IfNodeData continueCheckData = new IfNodeData();
        continueCheckData.setLabel("检查跳过");
        continueCheckData.setCondition(Expression.operator("==",
                Expression.operator("%", Expression.variable("i"), Expression.literal(2)),
                Expression.literal(0)));
        continueCheckData.setThenBranch("continueNode");
        continueCheckData.setElseBranch("addSum");

        VisualNode continueCheckNode = VisualNode.builder()
                .id("checkContinue")
                .type("if")
                .data(continueCheckData)
                .build();

        // continue节点
        ContinueNodeData continueData = new ContinueNodeData();
        continueData.setLabel("继续");

        VisualNode continueNode = VisualNode.builder()
                .id("continueNode")
                .type("continue")
                .data(continueData)
                .build();

        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        forNode,
                        breakCheckNode,
                        breakNode,
                        continueCheckNode,
                        continueNode,
                        createExpressionNode("addSum", "sum",
                                Expression.operator("+", Expression.variable("sum"), Expression.variable("i"))),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "for1"),
                        createEdgeWithHandle("e2", "for1", "checkBreak", "body", null),
                        createEdgeWithHandle("e3", "checkBreak", "breakNode", "then", null),
                        createEdgeWithHandle("e4", "checkBreak", "checkContinue", "else", null),
                        createEdgeWithHandle("e5", "checkContinue", "continueNode", "then", null),
                        createEdgeWithHandle("e6", "checkContinue", "addSum", "else", null),
                        createEdgeWithHandle("e7", "for1", "end", "next", null)
                ))
                .build();
    }
}

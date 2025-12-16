package com.ql.qlexpress.web.core.transpiler;

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
 * VisualToQLTranspiler 单元测试
 *
 * @author qlexpress
 */
class VisualToQLTranspilerTest {

    private VisualToQLTranspiler transpiler;

    @BeforeEach
    void setUp() {
        transpiler = new VisualToQLTranspiler();
    }

    @Nested
    @DisplayName("基础流程测试")
    class BasicFlowTest {

        @Test
        @DisplayName("空流程应返回空脚本")
        void transpile_emptyFlow_shouldReturnEmptyScript() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createEndNode("end")
                    ))
                    .edges(Collections.singletonList(
                            createEdge("e1", "start", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            // 空流程只有start和end，不生成实际代码
            assertEquals("", result.getScript().trim());
        }

        @Test
        @DisplayName("简单表达式流程应正确转译")
        void transpile_simpleExpression_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createExpressionNode("expr1", "result", Expression.operator("+",
                                    Expression.literal(1), Expression.literal(2))),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "expr1"),
                            createEdge("e2", "expr1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertTrue(result.getScript().contains("result = (1 + 2);"));
        }

        @Test
        @DisplayName("多个连续表达式应正确转译")
        void transpile_multipleExpressions_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "a", Expression.literal(10)),
                            createAssignmentNode("assign2", "b", Expression.literal(20)),
                            createExpressionNode("expr1", "sum", Expression.operator("+",
                                    Expression.variable("a"), Expression.variable("b"))),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "assign2"),
                            createEdge("e3", "assign2", "expr1"),
                            createEdge("e4", "expr1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("a = 10;"));
            assertTrue(script.contains("b = 20;"));
            assertTrue(script.contains("sum = (a + b);"));
        }
    }

    @Nested
    @DisplayName("函数调用测试")
    class FunctionCallTest {

        @Test
        @DisplayName("无参数函数调用应正确转译")
        void transpile_noArgsFunctionCall_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createFunctionCallNode("func1", "now", Collections.emptyList(), "currentTime"),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "func1"),
                            createEdge("e2", "func1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertTrue(result.getScript().contains("currentTime = now();"));
        }

        @Test
        @DisplayName("带参数函数调用应正确转译")
        void transpile_withArgsFunctionCall_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createFunctionCallNode("func1", "max",
                                    Arrays.asList(Expression.literal(1), Expression.literal(2), Expression.literal(3)),
                                    "maxValue"),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "func1"),
                            createEdge("e2", "func1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertTrue(result.getScript().contains("maxValue = max(1, 2, 3);"));
        }

        @Test
        @DisplayName("无返回值函数调用应正确转译")
        void transpile_noReturnFunctionCall_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createFunctionCallNode("func1", "println",
                                    Collections.singletonList(Expression.literal("Hello")), null),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "func1"),
                            createEdge("e2", "func1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertTrue(result.getScript().contains("println(\"Hello\");"));
            assertFalse(result.getScript().contains("= println"));
        }
    }

    @Nested
    @DisplayName("IF条件分支测试")
    class IfBranchTest {

        @Test
        @DisplayName("简单IF条件应正确转译")
        void transpile_simpleIf_shouldGenerateCorrectQL() {
            // 创建then分支节点
            VisualNode thenExpr = createExpressionNode("then1", "result",
                    Expression.literal("positive"));

            // 创建if节点
            IfNodeData ifData = new IfNodeData();
            ifData.setLabel("条件判断");
            ifData.setCondition(Expression.operator(">", Expression.variable("x"), Expression.literal(0)));
            ifData.setThenBranch("then1");

            VisualNode ifNode = VisualNode.builder()
                    .id("if1")
                    .type("if")
                    .data(ifData)
                    .build();

            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            ifNode,
                            thenExpr,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "if1"),
                            createEdgeWithHandle("e2", "if1", "then1", "then", null),
                            createEdge("e3", "then1", "end"),
                            createEdgeWithHandle("e4", "if1", "end", "next", null)
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("if ((x > 0)) {"));
            assertTrue(script.contains("result = \"positive\";"));
            assertTrue(script.contains("}"));
        }

        @Test
        @DisplayName("IF-ELSE条件应正确转译")
        void transpile_ifElse_shouldGenerateCorrectQL() {
            // 创建then和else分支节点
            VisualNode thenExpr = createExpressionNode("then1", "result",
                    Expression.literal("positive"));
            VisualNode elseExpr = createExpressionNode("else1", "result",
                    Expression.literal("non-positive"));

            // 创建if节点
            IfNodeData ifData = new IfNodeData();
            ifData.setLabel("条件判断");
            ifData.setCondition(Expression.operator(">", Expression.variable("x"), Expression.literal(0)));
            ifData.setThenBranch("then1");
            ifData.setElseBranch("else1");

            VisualNode ifNode = VisualNode.builder()
                    .id("if1")
                    .type("if")
                    .data(ifData)
                    .build();

            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            ifNode,
                            thenExpr,
                            elseExpr,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "if1"),
                            createEdgeWithHandle("e2", "if1", "then1", "then", null),
                            createEdgeWithHandle("e3", "if1", "else1", "else", null),
                            createEdge("e4", "then1", "end"),
                            createEdge("e5", "else1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("if ((x > 0)) {"));
            assertTrue(script.contains("} else {"));
        }
    }

    @Nested
    @DisplayName("FOR循环测试")
    class ForLoopTest {

        @Test
        @DisplayName("标准FOR循环应正确转译")
        void transpile_standardFor_shouldGenerateCorrectQL() {
            // 创建循环体节点
            VisualNode bodyExpr = createExpressionNode("body1", "sum",
                    Expression.operator("+", Expression.variable("sum"), Expression.variable("i")));

            // 创建for节点
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

            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            forNode,
                            bodyExpr,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "for1"),
                            createEdgeWithHandle("e2", "for1", "body1", "body", null),
                            createEdgeWithHandle("e3", "for1", "end", "next", null)
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("for (i = 0; (i < 10); i = i + 1) {"));
            assertTrue(script.contains("sum = (sum + i);"));
            assertTrue(script.contains("}"));
        }
    }

    @Nested
    @DisplayName("WHILE循环测试")
    class WhileLoopTest {

        @Test
        @DisplayName("WHILE循环应正确转译")
        void transpile_while_shouldGenerateCorrectQL() {
            // 创建循环体节点
            VisualNode bodyExpr = createAssignmentNode("body1", "count",
                    Expression.operator("-", Expression.variable("count"), Expression.literal(1)));

            // 创建while节点
            WhileNodeData whileData = new WhileNodeData();
            whileData.setLabel("循环");
            whileData.setCondition(Expression.operator(">", Expression.variable("count"), Expression.literal(0)));
            whileData.setBodyEntrance("body1");

            VisualNode whileNode = VisualNode.builder()
                    .id("while1")
                    .type("while")
                    .data(whileData)
                    .build();

            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            whileNode,
                            bodyExpr,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "while1"),
                            createEdgeWithHandle("e2", "while1", "body1", "body", null),
                            createEdgeWithHandle("e3", "while1", "end", "next", null)
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("while ((count > 0)) {"));
            assertTrue(script.contains("count = (count - 1);"));
            assertTrue(script.contains("}"));
        }
    }

    @Nested
    @DisplayName("Return语句测试")
    class ReturnTest {

        @Test
        @DisplayName("带返回值的return应正确转译")
        void transpile_returnWithValue_shouldGenerateCorrectQL() {
            // 创建return节点
            ReturnNodeData returnData = new ReturnNodeData();
            returnData.setLabel("返回");
            returnData.setValue(Expression.variable("result"));

            VisualNode returnNode = VisualNode.builder()
                    .id("return1")
                    .type("return")
                    .data(returnData)
                    .build();

            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "result", Expression.literal(42)),
                            returnNode,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "return1"),
                            createEdge("e3", "return1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("result = 42;"));
            assertTrue(script.contains("return result;"));
        }

        @Test
        @DisplayName("无返回值的return应正确转译")
        void transpile_returnWithoutValue_shouldGenerateCorrectQL() {
            ReturnNodeData returnData = new ReturnNodeData();
            returnData.setLabel("返回");

            VisualNode returnNode = VisualNode.builder()
                    .id("return1")
                    .type("return")
                    .data(returnData)
                    .build();

            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            returnNode,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "return1"),
                            createEdge("e2", "return1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertTrue(result.getScript().contains("return;"));
        }
    }

    @Nested
    @DisplayName("变量声明测试")
    class VariableDeclarationTest {

        @Test
        @DisplayName("带初始值的变量声明应正确转译")
        void transpile_variableWithInitialValue_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .variables(Arrays.asList(
                            VariableDefinition.builder()
                                    .name("x")
                                    .type("int")
                                    .initialValue(Expression.literal(10))
                                    .build(),
                            VariableDefinition.builder()
                                    .name("name")
                                    .type("String")
                                    .initialValue(Expression.literal("test"))
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

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("int x = 10;"));
            assertTrue(script.contains("String name = \"test\";"));
        }
    }

    @Nested
    @DisplayName("Import语句测试")
    class ImportTest {

        @Test
        @DisplayName("import语句应正确转译")
        void transpile_imports_shouldGenerateCorrectQL() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .imports(Arrays.asList("java.util.List", "java.util.Map"))
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createEndNode("end")
                    ))
                    .edges(Collections.singletonList(
                            createEdge("e1", "start", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            String script = result.getScript();
            assertTrue(script.contains("import java.util.List;"));
            assertTrue(script.contains("import java.util.Map;"));
        }
    }

    @Nested
    @DisplayName("转译选项测试")
    class TranspileOptionsTest {

        @Test
        @DisplayName("禁用格式化应生成紧凑代码")
        void transpile_noFormat_shouldGenerateCompactCode() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
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

            TranspileOptions options = TranspileOptions.builder()
                    .format(false)
                    .build();

            TranspileResult result = transpiler.transpile(schema, options);

            assertTrue(result.isSuccess());
            assertFalse(result.getScript().contains("\n"));
        }

        @Test
        @DisplayName("启用注释应生成注释")
        void transpile_withComments_shouldGenerateComments() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .variables(Collections.singletonList(
                            VariableDefinition.builder()
                                    .name("x")
                                    .type("int")
                                    .initialValue(Expression.literal(10))
                                    .description("计数器")
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

            TranspileOptions options = TranspileOptions.builder()
                    .generateComments(true)
                    .build();

            TranspileResult result = transpiler.transpile(schema, options);

            assertTrue(result.isSuccess());
            assertTrue(result.getScript().contains("// 变量声明"));
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTest {

        @Test
        @DisplayName("缺少起始节点应返回失败")
        void transpile_noStartNode_shouldFail() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.singletonList(
                            createEndNode("end")
                    ))
                    .edges(Collections.emptyList())
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("起始节点"));
        }

        @Test
        @DisplayName("空节点列表应返回失败")
        void transpile_emptyNodes_shouldFail() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.emptyList())
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertFalse(result.isSuccess());
        }

        @Test
        @DisplayName("null schema应返回失败")
        void transpile_nullSchema_shouldFail() {
            TranspileResult result = transpiler.transpile(null);

            assertFalse(result.isSuccess());
        }
    }

    @Nested
    @DisplayName("统计信息测试")
    class StatsTest {

        @Test
        @DisplayName("应正确统计节点和边数量")
        void transpile_shouldGenerateCorrectStats() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
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

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertNotNull(result.getStats());
            assertEquals(4, result.getStats().getNodeCount());
            assertEquals(3, result.getStats().getEdgeCount());
            assertTrue(result.getStats().getTranspileTimeMs() >= 0);
        }
    }

    @Nested
    @DisplayName("源映射测试")
    class SourceMappingTest {

        @Test
        @DisplayName("应生成正确的源映射")
        void transpile_shouldGenerateSourceMapping() {
            VisualFlowSchema schema = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createAssignmentNode("assign1", "a", Expression.literal(1)),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            TranspileResult result = transpiler.transpile(schema);

            assertTrue(result.isSuccess());
            assertNotNull(result.getSourceMap());
            assertTrue(result.getSourceMap().containsKey("assign1"));
        }
    }

    // ==================== 辅助方法 ====================

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

    private VisualNode createFunctionCallNode(String id, String functionName,
                                               java.util.List<Expression> arguments, String resultVariable) {
        FunctionCallNodeData data = new FunctionCallNodeData();
        data.setLabel("函数调用");
        data.setFunctionName(functionName);
        data.setArguments(arguments);
        data.setResultVariable(resultVariable);
        return VisualNode.builder()
                .id(id)
                .type("function_call")
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

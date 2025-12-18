package com.ql.qlexpress.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ql.qlexpress.web.core.transpiler.TranspileOptions;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.model.dto.TranspileToQLRequest;
import com.ql.qlexpress.web.model.enums.EdgeType;
import com.ql.qlexpress.web.model.visual.*;
import com.ql.qlexpress.web.service.TranspileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TranspileController 单元测试
 *
 * @author qlexpress
 */
@WebMvcTest(TranspileController.class)
@Import({TranspileService.class, VisualToQLTranspiler.class})
class TranspileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/v1/transpile/to-ql 测试")
    class TranspileToQLTest {

        @Test
        @DisplayName("有效请求应返回成功响应")
        void transpileToQL_validRequest_shouldReturnSuccess() throws Exception {
            TranspileToQLRequest request = createValidRequest();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.script").isNotEmpty())
                    .andExpect(jsonPath("$.data.stats.nodeCount").value(3))
                    .andExpect(jsonPath("$.data.stats.edgeCount").value(2));
        }

        @Test
        @DisplayName("带选项的请求应正确处理")
        void transpileToQL_withOptions_shouldApplyOptions() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(createSimpleFlow())
                    .options(TranspileOptions.builder()
                            .format(true)
                            .generateComments(true)
                            .build())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.script").isNotEmpty());
        }

        @Test
        @DisplayName("空流程应返回错误")
        void transpileToQL_emptyFlow_shouldReturnError() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(VisualFlowSchema.builder()
                            .version("1.0.0")
                            .nodes(Collections.emptyList())
                            .build())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("null流程应返回验证错误")
        void transpileToQL_nullFlow_shouldReturnValidationError() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(null)
                    .build();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transpile/quick 测试")
    class QuickTranspileTest {

        @Test
        @DisplayName("快速转译应仅返回脚本")
        void quickTranspile_validRequest_shouldReturnScriptOnly() throws Exception {
            TranspileToQLRequest request = createValidRequest();

            mockMvc.perform(post("/api/v1/transpile/quick")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isString())
                    .andExpect(jsonPath("$.data", containsString("result = (1 + 2);")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transpile/formatted 测试")
    class FormattedTranspileTest {

        @Test
        @DisplayName("格式化转译应返回带格式的脚本")
        void transpileFormatted_validRequest_shouldReturnFormattedScript() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(createFlowWithVariables())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/formatted")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isString())
                    .andExpect(jsonPath("$.data", containsString("// 变量声明")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transpile/compact 测试")
    class CompactTranspileTest {

        @Test
        @DisplayName("紧凑转译应返回无换行的脚本")
        void transpileCompact_validRequest_shouldReturnCompactScript() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(createMultiStatementFlow())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/compact")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isString())
                    .andExpect(jsonPath("$.data", not(containsString("\n"))));
        }
    }

    @Nested
    @DisplayName("复杂流程转译测试")
    class ComplexFlowTest {

        @Test
        @DisplayName("带IF分支的流程应正确转译")
        void transpile_ifBranch_shouldGenerateCorrectQL() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(createIfBranchFlow())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.script", containsString("if ((x > 0))")));
        }

        @Test
        @DisplayName("带FOR循环的流程应正确转译")
        void transpile_forLoop_shouldGenerateCorrectQL() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(createForLoopFlow())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.script", containsString("for (")));
        }

        @Test
        @DisplayName("带函数调用的流程应正确转译")
        void transpile_functionCall_shouldGenerateCorrectQL() throws Exception {
            TranspileToQLRequest request = TranspileToQLRequest.builder()
                    .flow(createFunctionCallFlow())
                    .build();

            mockMvc.perform(post("/api/v1/transpile/to-ql")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.script", containsString("max(1, 2, 3)")));
        }
    }

    // ==================== 辅助方法 ====================

    private TranspileToQLRequest createValidRequest() {
        return TranspileToQLRequest.builder()
                .flow(createSimpleFlow())
                .build();
    }

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

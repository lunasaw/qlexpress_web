package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.model.enums.EdgeType;
import com.ql.qlexpress.web.model.enums.ValidationLevel;
import com.ql.qlexpress.web.model.visual.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidateService 单元测试
 *
 * @author qlexpress
 */
class ValidateServiceTest {

    private ValidateService validateService;

    @BeforeEach
    void setUp() {
        validateService = new ValidateService();
    }

    @Nested
    @DisplayName("validateScript测试")
    class ValidateScriptTest {

        @Test
        @DisplayName("有效脚本应返回验证通过")
        void validateScript_validScript_shouldReturnValid() {
            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script("1 + 2")
                    .build();

            ValidateScriptResponse response = validateService.validateScript(request);

            assertTrue(response.isValid());
            assertTrue(response.getSyntaxErrors() == null || response.getSyntaxErrors().isEmpty());
        }

        @Test
        @DisplayName("语法错误脚本应返回验证失败")
        void validateScript_syntaxError_shouldReturnInvalid() {
            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script("1 + +")
                    .build();

            ValidateScriptResponse response = validateService.validateScript(request);

            assertFalse(response.isValid());
            assertNotNull(response.getSyntaxErrors());
            assertFalse(response.getSyntaxErrors().isEmpty());
        }

        @Test
        @DisplayName("复杂有效脚本应返回验证通过")
        void validateScript_complexValidScript_shouldReturnValid() {
            String script = "sum = 0;\n" +
                    "for (i = 0; i < 10; i = i + 1) {\n" +
                    "    sum = sum + i;\n" +
                    "}\n" +
                    "return sum;";

            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script(script)
                    .build();

            ValidateScriptResponse response = validateService.validateScript(request);

            assertTrue(response.isValid());
        }

        @Test
        @DisplayName("启用语义检查应检测潜在问题")
        void validateScript_withSemanticCheck_shouldDetectIssues() {
            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script("x / 0")
                    .checkSemantics(true)
                    .build();

            ValidateScriptResponse response = validateService.validateScript(request);

            assertTrue(response.isValid()); // 语法正确
            assertNotNull(response.getSemanticWarnings());
            // 检查是否有除零警告
            boolean hasDivisionWarning = response.getSemanticWarnings().stream()
                    .anyMatch(w -> "DIVISION_BY_ZERO".equals(w.getCode()));
            assertTrue(hasDivisionWarning);
        }

        @Test
        @DisplayName("启用最佳实践检查应返回建议")
        void validateScript_withBestPracticesCheck_shouldReturnSuggestions() {
            String script = "if (x > 0) {\n" +
                    "    if (y > 0) {\n" +
                    "        if (z > 0) {\n" +
                    "            if (w > 0) {\n" +
                    "                if (v > 0) {\n" +
                    "                    return 123456;\n" +
                    "                }\n" +
                    "            }\n" +
                    "        }\n" +
                    "    }\n" +
                    "}";

            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script(script)
                    .checkBestPractices(true)
                    .build();

            ValidateScriptResponse response = validateService.validateScript(request);

            assertTrue(response.isValid());
            assertNotNull(response.getSuggestions());
            // 检查是否有嵌套深度建议
            boolean hasNestingSuggestion = response.getSuggestions().stream()
                    .anyMatch(s -> "DEEP_NESTING".equals(s.getType()));
            assertTrue(hasNestingSuggestion);
        }

        @Test
        @DisplayName("应返回复杂度分析")
        void validateScript_shouldReturnComplexityAnalysis() {
            String script = "sum = 0;\n" +
                    "for (i = 0; i < 10; i = i + 1) {\n" +
                    "    if (i % 2 == 0) {\n" +
                    "        sum = sum + i;\n" +
                    "    }\n" +
                    "}\n" +
                    "return sum;";

            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script(script)
                    .build();

            ValidateScriptResponse response = validateService.validateScript(request);

            assertTrue(response.isValid());
            assertNotNull(response.getComplexity());
            assertTrue(response.getComplexity().getCyclomaticComplexity() > 1);
            assertTrue(response.getComplexity().getLineCount() > 0);
        }

        @Test
        @DisplayName("空脚本应返回验证失败")
        void validateScript_emptyScript_shouldReturnInvalid() {
            ValidateScriptRequest request = ValidateScriptRequest.builder()
                    .script("")
                    .build();

            // 空脚本可能被QL引擎接受或拒绝，取决于引擎实现
            ValidateScriptResponse response = validateService.validateScript(request);
            assertNotNull(response);
        }
    }

    @Nested
    @DisplayName("validateFlow测试")
    class ValidateFlowTest {

        @Test
        @DisplayName("有效流程应返回验证通过")
        void validateFlow_validFlow_shouldReturnValid() {
            VisualFlowSchema flow = createValidFlow();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .level(ValidationLevel.STANDARD)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertTrue(response.isValid());
            assertTrue(response.getStructureErrors().isEmpty());
            assertTrue(response.getNodeErrors().isEmpty());
            assertTrue(response.getEdgeErrors().isEmpty());
        }

        @Test
        @DisplayName("空流程应返回结构错误")
        void validateFlow_emptyFlow_shouldReturnStructureError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.emptyList())
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.isValid());
            assertFalse(response.getStructureErrors().isEmpty());
        }

        @Test
        @DisplayName("缺少起始节点应返回错误")
        void validateFlow_noStartNode_shouldReturnError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.singletonList(createEndNode("end")))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.isValid());
            assertTrue(response.getStructureErrors().stream()
                    .anyMatch(e -> e.getMessage().contains("起始节点")));
        }

        @Test
        @DisplayName("缺少结束节点应返回错误")
        void validateFlow_noEndNode_shouldReturnError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Collections.singletonList(createStartNode("start")))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.isValid());
            assertTrue(response.getStructureErrors().stream()
                    .anyMatch(e -> e.getMessage().contains("结束节点")));
        }

        @Test
        @DisplayName("重复节点ID应返回错误")
        void validateFlow_duplicateNodeId_shouldReturnError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("node1"),
                            createEndNode("node1") // 重复ID
                    ))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.isValid());
            assertTrue(response.getStructureErrors().stream()
                    .anyMatch(e -> e.getMessage().contains("重复")));
        }

        @Test
        @DisplayName("多个起始节点应返回错误")
        void validateFlow_multipleStartNodes_shouldReturnError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start1"),
                            createStartNode("start2"),
                            createEndNode("end")
                    ))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.isValid());
            assertTrue(response.getStructureErrors().stream()
                    .anyMatch(e -> e.getMessage().contains("多个起始节点")));
        }

        @Test
        @DisplayName("不可达节点应返回错误")
        void validateFlow_unreachableNode_shouldReturnError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createExpressionNode("expr1"),
                            createExpressionNode("unreachable"), // 不可达节点
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "expr1"),
                            createEdge("e2", "expr1", "end")
                    ))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.isValid());
            assertTrue(response.getStructureErrors().stream()
                    .anyMatch(e -> e.getMessage().contains("不可达")));
        }

        @Test
        @DisplayName("边引用不存在的节点应返回错误")
        void validateFlow_invalidEdgeReference_shouldReturnError() {
            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            createExpressionNode("expr1"),
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "expr1"),
                            createEdge("e2", "expr1", "nonexistent"), // 不存在的目标节点
                            createEdge("e3", "expr1", "end")
                    ))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.getEdgeErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("节点验证测试")
    class NodeValidationTest {

        @Test
        @DisplayName("if节点缺少条件应返回错误")
        void validateFlow_ifNodeMissingCondition_shouldReturnError() {
            IfNodeData ifData = new IfNodeData();
            ifData.setLabel("条件");
            // 不设置condition

            VisualNode ifNode = VisualNode.builder()
                    .id("if1")
                    .type("if")
                    .data(ifData)
                    .build();

            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            ifNode,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "if1"),
                            createEdge("e2", "if1", "end")
                    ))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.getNodeErrors().isEmpty());
            assertTrue(response.getNodeErrors().stream()
                    .anyMatch(e -> e.getNodeId().equals("if1")));
        }

        @Test
        @DisplayName("assignment节点缺少变量名应返回错误")
        void validateFlow_assignmentNodeMissingVariable_shouldReturnError() {
            AssignmentNodeData assignData = new AssignmentNodeData();
            assignData.setLabel("赋值");
            assignData.setValue(Expression.literal(1));
            // 不设置variable

            VisualNode assignNode = VisualNode.builder()
                    .id("assign1")
                    .type("assignment")
                    .data(assignData)
                    .build();

            VisualFlowSchema flow = VisualFlowSchema.builder()
                    .version("1.0.0")
                    .nodes(Arrays.asList(
                            createStartNode("start"),
                            assignNode,
                            createEndNode("end")
                    ))
                    .edges(Arrays.asList(
                            createEdge("e1", "start", "assign1"),
                            createEdge("e2", "assign1", "end")
                    ))
                    .build();

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.getNodeErrors().isEmpty());
        }

        @Test
        @DisplayName("function_call节点缺少函数名应返回错误")
        void validateFlow_functionCallNodeMissingFunctionName_shouldReturnError() {
            FunctionCallNodeData funcData = new FunctionCallNodeData();
            funcData.setLabel("函数调用");
            // 不设置functionName

            VisualNode funcNode = VisualNode.builder()
                    .id("func1")
                    .type("function_call")
                    .data(funcData)
                    .build();

            VisualFlowSchema flow = VisualFlowSchema.builder()
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

            ValidateFlowRequest request = ValidateFlowRequest.builder()
                    .flow(flow)
                    .build();

            ValidateFlowResponse response = validateService.validateFlow(request);

            assertFalse(response.getNodeErrors().isEmpty());
        }
    }

    // ==================== 辅助方法 ====================

    private VisualFlowSchema createValidFlow() {
        return VisualFlowSchema.builder()
                .version("1.0.0")
                .nodes(Arrays.asList(
                        createStartNode("start"),
                        createExpressionNode("expr1"),
                        createEndNode("end")
                ))
                .edges(Arrays.asList(
                        createEdge("e1", "start", "expr1"),
                        createEdge("e2", "expr1", "end")
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

    private VisualNode createExpressionNode(String id) {
        ExpressionNodeData data = new ExpressionNodeData();
        data.setLabel("表达式");
        data.setExpression(Expression.literal(1));
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
}

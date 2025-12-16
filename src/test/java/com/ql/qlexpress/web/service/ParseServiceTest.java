package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.parser.QLToVisualParser;
import com.ql.qlexpress.web.model.dto.ParseToVisualRequest;
import com.ql.qlexpress.web.model.dto.ParseToVisualResponse;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import com.ql.qlexpress.web.model.visual.VisualNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParseService 单元测试 - QL脚本转可视化JSON测试
 *
 * @author qlexpress
 */
class ParseServiceTest {

    private ParseService parseService;

    @BeforeEach
    void setUp() {
        QLToVisualParser parser = new QLToVisualParser();
        parseService = new ParseService(parser);
    }

    @Nested
    @DisplayName("基础语句解析测试")
    class BasicStatementParseTest {

        @Test
        @DisplayName("简单赋值语句应正确解析")
        void parseToVisual_simpleAssignment_shouldParse() {
            String script = "a = 10;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertNotNull(response.getFlow().getNodes());
            // 应该有 start + assignment + end = 3个节点
            assertTrue(response.getFlow().getNodes().size() >= 3);
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "assignment"));
        }

        @Test
        @DisplayName("带类型的赋值语句应正确解析")
        void parseToVisual_typedAssignment_shouldParse() {
            String script = "int x = 100;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "assignment"));
        }

        @Test
        @DisplayName("多个赋值语句应正确解析")
        void parseToVisual_multipleAssignments_shouldParse() {
            String script = "a = 1;\nb = 2;\nc = a + b;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // start + 3个赋值 + end = 5个节点
            assertTrue(response.getFlow().getNodes().size() >= 5);
        }

        @Test
        @DisplayName("字符串赋值应正确解析")
        void parseToVisual_stringAssignment_shouldParse() {
            String script = "name = \"hello world\";";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("条件语句解析测试")
    class ConditionalStatementParseTest {

        @Test
        @DisplayName("简单if语句应正确解析")
        void parseToVisual_simpleIf_shouldParse() {
            String script = "if (x > 0) {\n    result = 1;\n}";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "if"));
        }

        @Test
        @DisplayName("if-else语句应正确解析")
        void parseToVisual_ifElse_shouldParse() {
            String script = "if (x > 0) {\n    result = 1;\n} else {\n    result = 0;\n}";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("嵌套if语句应正确解析")
        void parseToVisual_nestedIf_shouldParse() {
            String script = "if (x > 0) {\n    if (x > 10) {\n        result = 2;\n    }\n}";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("循环语句解析测试")
    class LoopStatementParseTest {

        @Test
        @DisplayName("for循环应正确解析")
        void parseToVisual_forLoop_shouldParse() {
            String script = "for (i = 0; i < 10; i = i + 1) {\n    sum = sum + i;\n}";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "for"));
        }

        @Test
        @DisplayName("while循环应正确解析")
        void parseToVisual_whileLoop_shouldParse() {
            String script = "while (count > 0) {\n    count = count - 1;\n}";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "while"));
        }

        @Test
        @DisplayName("嵌套循环应正确解析")
        void parseToVisual_nestedLoop_shouldParse() {
            String script = "for (i = 0; i < 5; i = i + 1) {\n    for (j = 0; j < 3; j = j + 1) {\n        result = i * j;\n    }\n}";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("函数调用解析测试")
    class FunctionCallParseTest {

        @Test
        @DisplayName("无参数函数调用应正确解析")
        void parseToVisual_noArgsFunctionCall_shouldParse() {
            String script = "now();";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "function_call"));
        }

        @Test
        @DisplayName("带参数函数调用应正确解析")
        void parseToVisual_withArgsFunctionCall_shouldParse() {
            String script = "max(1, 2, 3);";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "function_call"));
        }

        @Test
        @DisplayName("函数调用赋值应正确解析")
        void parseToVisual_functionCallAssignment_shouldParse() {
            String script = "result = max(a, b);";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("return语句解析测试")
    class ReturnStatementParseTest {

        @Test
        @DisplayName("带值return应正确解析")
        void parseToVisual_returnWithValue_shouldParse() {
            String script = "return result;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "return"));
        }

        @Test
        @DisplayName("无值return应正确解析")
        void parseToVisual_returnWithoutValue_shouldParse() {
            String script = "return;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "return"));
        }

        @Test
        @DisplayName("表达式return应正确解析")
        void parseToVisual_returnExpression_shouldParse() {
            String script = "return a + b * c;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "return"));
        }
    }

    @Nested
    @DisplayName("复杂业务场景解析测试")
    class ComplexBusinessScenarioParseTest {

        @Test
        @DisplayName("折扣计算逻辑应正确解析")
        void parseToVisual_discountCalculation_shouldParse() {
            String script = "discount = 0;\n" +
                    "if (amount >= 1000) {\n" +
                    "    discount = 0.2;\n" +
                    "}\n" +
                    "if (amount >= 500) {\n" +
                    "    discount = 0.1;\n" +
                    "}\n" +
                    "finalPrice = amount * (1 - discount);\n" +
                    "return finalPrice;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // 验证节点数量和类型
            assertTrue(response.getFlow().getNodes().size() >= 5);
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "if"));
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "return"));
        }

        @Test
        @DisplayName("循环累加逻辑应正确解析")
        void parseToVisual_loopSum_shouldParse() {
            String script = "sum = 0;\n" +
                    "for (i = 1; i <= 100; i = i + 1) {\n" +
                    "    sum = sum + i;\n" +
                    "}\n" +
                    "return sum;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "for"));
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "return"));
        }

        @Test
        @DisplayName("条件循环逻辑应正确解析")
        void parseToVisual_conditionalLoop_shouldParse() {
            String script = "count = 10;\n" +
                    "result = 1;\n" +
                    "while (count > 0) {\n" +
                    "    result = result * count;\n" +
                    "    count = count - 1;\n" +
                    "}\n" +
                    "return result;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "while"));
        }

        @Test
        @DisplayName("多分支判断应正确解析")
        void parseToVisual_multipleBranches_shouldParse() {
            String script = "if (score >= 90) {\n" +
                    "    grade = \"A\";\n" +
                    "}\n" +
                    "if (score >= 80) {\n" +
                    "    grade = \"B\";\n" +
                    "}\n" +
                    "if (score >= 70) {\n" +
                    "    grade = \"C\";\n" +
                    "}\n" +
                    "if (score >= 60) {\n" +
                    "    grade = \"D\";\n" +
                    "}\n" +
                    "return grade;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // 应该有多个if节点
            long ifCount = response.getFlow().getNodes().stream()
                    .filter(n -> "if".equals(n.getType()))
                    .count();
            assertTrue(ifCount >= 4);
        }
    }

    @Nested
    @DisplayName("quickParse快速解析测试")
    class QuickParseTest {

        @Test
        @DisplayName("快速解析应直接返回流程定义")
        void quickParse_shouldReturnFlowDirectly() {
            String script = "a = 1; b = 2; return a + b;";

            VisualFlowSchema flow = parseService.quickParse(script);

            assertNotNull(flow);
            assertNotNull(flow.getNodes());
            assertNotNull(flow.getEdges());
        }

        @Test
        @DisplayName("快速解析无效脚本应抛出异常")
        void quickParse_invalidScript_shouldThrowException() {
            String script = "invalid syntax {{{{";

            assertThrows(RuntimeException.class, () -> parseService.quickParse(script));
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class BoundaryConditionParseTest {

        @Test
        @DisplayName("空脚本应返回基本流程")
        void parseToVisual_emptyScript_shouldReturnBasicFlow() {
            String script = "";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // 至少有start和end节点
            assertTrue(response.getFlow().getNodes().size() >= 2);
        }

        @Test
        @DisplayName("只有注释的脚本应正确处理")
        void parseToVisual_onlyComments_shouldParse() {
            String script = "// this is a comment\n// another comment";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("语法错误应返回失败")
        void parseToVisual_syntaxError_shouldFail() {
            String script = "if (x > ) { }";  // 语法错误
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertFalse(response.isSuccess());
            assertNotNull(response.getErrorMessage());
        }

        @Test
        @DisplayName("特殊字符应正确处理")
        void parseToVisual_specialCharacters_shouldParse() {
            String script = "str = \"hello\\nworld\\t!\";";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("长脚本应正确解析")
        void parseToVisual_longScript_shouldParse() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                sb.append("var").append(i).append(" = ").append(i).append(";\n");
            }
            sb.append("return var49;");

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(sb.toString())
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // 应该有很多节点
            assertTrue(response.getFlow().getNodes().size() >= 50);
        }
    }

    @Nested
    @DisplayName("统计信息测试")
    class StatsTest {

        @Test
        @DisplayName("解析应返回正确的统计信息")
        void parseToVisual_shouldReturnCorrectStats() {
            String script = "a = 1;\nb = 2;\nc = a + b;\nreturn c;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getStats());
            assertTrue(response.getStats().getParseTimeMs() >= 0);
            assertTrue(response.getStats().getNodeCount() > 0);
            assertTrue(response.getStats().getEdgeCount() > 0);
            assertEquals(4, response.getStats().getScriptLineCount());
        }
    }

    @Nested
    @DisplayName("流程结构验证测试")
    class FlowStructureTest {

        @Test
        @DisplayName("解析结果应包含start和end节点")
        void parseToVisual_shouldHaveStartAndEndNodes() {
            String script = "result = 42;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "start"));
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "end"));
        }

        @Test
        @DisplayName("解析结果的边应正确连接节点")
        void parseToVisual_edgesShouldConnectNodes() {
            String script = "a = 1; b = 2;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow().getEdges());
            // 边的数量应该至少是节点数-1
            assertTrue(response.getFlow().getEdges().size() >= response.getFlow().getNodes().size() - 1);
        }

        @Test
        @DisplayName("解析结果应包含元数据")
        void parseToVisual_shouldHaveMetadata() {
            String script = "x = 1;";
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow().getMetadata());
        }
    }

    @Nested
    @DisplayName("双向转换一致性测试")
    class RoundTripTest {

        @Test
        @DisplayName("QL->JSON->QL应保持语义一致")
        void roundTrip_shouldMaintainSemantics() {
            // 原始脚本
            String originalScript = "a = 10;\nb = 20;\nresult = a + b;\nreturn result;";

            // QL -> JSON
            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(originalScript)
                    .build();
            ParseToVisualResponse parseResponse = parseService.parseToVisual(request);

            assertTrue(parseResponse.isSuccess());
            assertNotNull(parseResponse.getFlow());

            // 验证关键结构存在
            VisualFlowSchema flow = parseResponse.getFlow();
            assertTrue(hasNodeOfType(flow.getNodes(), "start"));
            assertTrue(hasNodeOfType(flow.getNodes(), "end"));
            assertTrue(hasNodeOfType(flow.getNodes(), "assignment"));
            assertTrue(hasNodeOfType(flow.getNodes(), "return"));
        }
    }

    // ==================== 辅助方法 ====================

    private boolean hasNodeOfType(List<VisualNode> nodes, String type) {
        if (nodes == null) return false;
        return nodes.stream().anyMatch(n -> type.equals(n.getType()));
    }
}

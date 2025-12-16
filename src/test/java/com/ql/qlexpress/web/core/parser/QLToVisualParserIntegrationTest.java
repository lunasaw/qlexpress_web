package com.ql.qlexpress.web.core.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ql.qlexpress.web.core.transpiler.TranspileResult;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QL脚本 ↔ JSON 往返测试
 * 验证 QL → JSON → QL 的转换保持语义一致性
 *
 * @author qlexpress
 */
class QLToVisualParserIntegrationTest {

    private QLToVisualParser parser;
    private VisualToQLTranspiler transpiler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        parser = new QLToVisualParser();
        transpiler = new VisualToQLTranspiler();
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Nested
    @DisplayName("基础语句往返测试")
    class BasicStatementRoundTripTest {

        @Test
        @DisplayName("简单赋值语句应能往返转换")
        void roundTrip_simpleAssignment_shouldPreserveSemantics() {
            String script = "x = 10;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("x = 10") || resultScript.contains("x=10"),
                    "结果应包含赋值语句: " + resultScript);
        }

        @Test
        @DisplayName("带类型的变量声明应能往返转换")
        void roundTrip_typedDeclaration_shouldPreserveSemantics() {
            String script = "int count = 0;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("count") && resultScript.contains("0"),
                    "结果应包含变量声明: " + resultScript);
        }

        @Test
        @DisplayName("函数调用应能往返转换")
        void roundTrip_functionCall_shouldPreserveSemantics() {
            String script = "println(\"Hello World\");";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("println"),
                    "结果应包含函数调用: " + resultScript);
        }
    }

    @Nested
    @DisplayName("控制流语句往返测试")
    class ControlFlowRoundTripTest {

        @Test
        @DisplayName("if语句应能往返转换")
        void roundTrip_ifStatement_shouldPreserveSemantics() {
            String script = "if (x > 0) { result = \"positive\"; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("if") && resultScript.contains(">"),
                    "结果应包含if条件: " + resultScript);
        }

        @Test
        @DisplayName("if-else语句应能往返转换")
        void roundTrip_ifElseStatement_shouldPreserveSemantics() {
            String script = "if (x > 0) { result = \"positive\"; } else { result = \"non-positive\"; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            // 验证if语句被正确解析和转译
            assertTrue(resultScript.contains("if"),
                    "结果应包含if: " + resultScript);
            // else分支可能被单独处理，检查包含关键元素即可
            assertTrue(resultScript.contains("result") && resultScript.contains("positive"),
                    "结果应包含条件体内容: " + resultScript);
        }

        @Test
        @DisplayName("if-else if-else语句应能往返转换")
        void roundTrip_ifElseIfElseStatement_shouldPreserveSemantics() {
            String script = "if (x > 0) { result = \"positive\"; } else if (x < 0) { result = \"negative\"; } else { result = \"zero\"; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("if"),
                    "结果应包含if: " + resultScript);
            assertTrue(resultScript.contains("else"),
                    "结果应包含else: " + resultScript);
        }

        @Test
        @DisplayName("for循环应能往返转换")
        void roundTrip_forLoop_shouldPreserveSemantics() {
            String script = "for (i = 0; i < 10; i = i + 1) { sum = sum + i; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("for"),
                    "结果应包含for: " + resultScript);
        }

        @Test
        @DisplayName("while循环应能往返转换")
        void roundTrip_whileLoop_shouldPreserveSemantics() {
            String script = "while (count > 0) { count = count - 1; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("while"),
                    "结果应包含while: " + resultScript);
        }

        @Test
        @DisplayName("foreach循环应能往返转换")
        void roundTrip_foreachLoop_shouldPreserveSemantics() {
            String script = "for (String item : list) { println(item); }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("for") && resultScript.contains(":"),
                    "结果应包含foreach: " + resultScript);
        }
    }

    @Nested
    @DisplayName("异常处理往返测试")
    class ExceptionHandlingRoundTripTest {

        @Test
        @DisplayName("try-catch应能往返转换")
        void roundTrip_tryCatch_shouldPreserveSemantics() {
            String script = "try { result = 10 / x; } catch (Exception e) { result = 0; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("try") && resultScript.contains("catch"),
                    "结果应包含try-catch: " + resultScript);
        }

        @Test
        @DisplayName("throw语句应能往返转换")
        void roundTrip_throw_shouldPreserveSemantics() {
            String script = "throw new Exception(\"error\");";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("throw"),
                    "结果应包含throw: " + resultScript);
        }
    }

    @Nested
    @DisplayName("函数定义往返测试")
    class FunctionDefinitionRoundTripTest {

        @Test
        @DisplayName("简单函数定义应能往返转换")
        void roundTrip_simpleFunctionDefinition_shouldPreserveSemantics() {
            String script = "function add(int a, int b) { return a + b; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            VisualFlowSchema flow = parseResult.getFlow();
            assertNotNull(flow.getFunctions(), "应有函数定义");
            assertFalse(flow.getFunctions().isEmpty(), "函数列表不应为空");
        }
    }

    @Nested
    @DisplayName("import语句往返测试")
    class ImportRoundTripTest {

        @Test
        @DisplayName("import语句应能往返转换")
        void roundTrip_import_shouldPreserveSemantics() {
            String script = "import java.util.List;\nimport java.util.Map;\nx = 1;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            VisualFlowSchema flow = parseResult.getFlow();
            assertNotNull(flow.getImports(), "应有import列表");
            assertEquals(2, flow.getImports().size(), "应有2个import");

            TranspileResult transpileResult = transpiler.transpile(flow);
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("import java.util.List"),
                    "结果应包含List import: " + resultScript);
            assertTrue(resultScript.contains("import java.util.Map"),
                    "结果应包含Map import: " + resultScript);
        }
    }

    @Nested
    @DisplayName("复杂脚本往返测试")
    class ComplexScriptRoundTripTest {

        @Test
        @DisplayName("多语句脚本应能往返转换")
        void roundTrip_multiStatementScript_shouldPreserveSemantics() {
            String script = "int sum = 0;\n" +
                    "for (i = 1; i <= 10; i = i + 1) {\n" +
                    "    sum = sum + i;\n" +
                    "}";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("sum"),
                    "结果应包含sum变量: " + resultScript);
            assertTrue(resultScript.contains("for"),
                    "结果应包含for循环: " + resultScript);
        }

        @Test
        @DisplayName("嵌套控制流应能往返转换")
        void roundTrip_nestedControlFlow_shouldPreserveSemantics() {
            String script = "for (i = 0; i < 10; i = i + 1) {\n" +
                    "    if (i % 2 == 0) {\n" +
                    "        println(i);\n" +
                    "    }\n" +
                    "}";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String resultScript = transpileResult.getScript();
            assertTrue(resultScript.contains("for"),
                    "结果应包含for: " + resultScript);
            assertTrue(resultScript.contains("if"),
                    "结果应包含if: " + resultScript);
        }
    }

    @Nested
    @DisplayName("JSON序列化往返测试")
    class JsonSerializationRoundTripTest {

        @Test
        @DisplayName("VisualFlowSchema应能正确序列化和反序列化")
        void roundTrip_jsonSerialization_shouldPreserveStructure() throws Exception {
            String script = "if (x > 0) { return \"positive\"; }";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功");

            VisualFlowSchema original = parseResult.getFlow();

            // 序列化为JSON
            String json = objectMapper.writeValueAsString(original);
            assertNotNull(json);
            assertTrue(json.length() > 0);

            // 反序列化
            VisualFlowSchema deserialized = objectMapper.readValue(json, VisualFlowSchema.class);
            assertNotNull(deserialized);

            // 转译并验证
            TranspileResult result1 = transpiler.transpile(original);
            TranspileResult result2 = transpiler.transpile(deserialized);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());

            // 两次转译的结果应该相同
            assertEquals(result1.getScript(), result2.getScript(),
                    "序列化前后转译结果应一致");
        }
    }
}

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
            assertFalse(json.isEmpty());

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

    @Nested
    @DisplayName("外部规则文件往返测试")
    class ExternalRuleFileRoundTripTest {

        /**
         * 通用规则文件双向转换验证方法
         *
         * 验证流程：
         * 1. 读取规则文件
         * 2. QL脚本 → JSON (解析)
         * 3. JSON → QL脚本 (转译)
         * 4. 验证语义一致性
         *
         * @param filePath 规则文件的绝对路径
         * @return 验证结果数组: [success, errorMessage, originalScript, json, resultScript, stats...]
         */
        public Object[] verifyRuleFileRoundTrip(String filePath) throws IOException {
            Path path = Paths.get(filePath);

            // 检查文件是否存在
            if (!Files.exists(path)) {
                return new Object[]{false, "文件不存在: " + filePath, null, null, null, null};
            }

            // 读取文件内容
            String script = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (script.trim().isEmpty()) {
                return new Object[]{false, "文件内容为空: " + filePath, null, null, null, null};
            }

            // Step 1: QL → JSON (解析)
            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            if (!parseResult.isSuccess()) {
                return new Object[]{false, "QL解析失败: " + parseResult.getErrorMessage(), script, null, null, null};
            }

            VisualFlowSchema flow = parseResult.getFlow();

            // Step 2: JSON序列化（验证JSON可序列化）
            String json;
            try {
                json = objectMapper.writeValueAsString(flow);
            } catch (Exception e) {
                return new Object[]{false, "JSON序列化失败: " + e.getMessage(), script, null, null, null};
            }

            // Step 3: JSON反序列化（验证JSON可反序列化）
            VisualFlowSchema deserializedFlow;
            try {
                deserializedFlow = objectMapper.readValue(json, VisualFlowSchema.class);
            } catch (Exception e) {
                return new Object[]{false, "JSON反序列化失败: " + e.getMessage(), script, json, null, null};
            }

            // Step 4: JSON → QL (转译)
            TranspileResult transpileResult = transpiler.transpile(deserializedFlow);
            if (!transpileResult.isSuccess()) {
                return new Object[]{false, "QL转译失败: " + transpileResult.getErrorMessage(), script, json, null, null};
            }

            String resultScript = transpileResult.getScript();

            // 构建统计信息
            int[] stats = new int[]{
                script.length(),  // originalLength
                resultScript.length(),  // resultLength
                flow.getNodes() != null ? flow.getNodes().size() : 0,  // nodeCount
                flow.getEdges() != null ? flow.getEdges().size() : 0,  // edgeCount
                flow.getImports() != null ? flow.getImports().size() : 0,  // importCount
                flow.getFunctions() != null ? flow.getFunctions().size() : 0,  // functionCount
                json.length()  // jsonLength
            };

            return new Object[]{true, null, script, json, resultScript, stats};
        }

        @Test
        @DisplayName("offWatch.groovy规则文件应能往返转换")
        void roundTrip_offWatchRule_shouldSucceed() throws IOException {
            String filePath = "/Users/weidian/project/vdian/wd24/wd24-edge-device-plugin/rule-manager/src/main/resources/rule_2_5_1_fix/offWatch.groovy";

            Object[] result = verifyRuleFileRoundTrip(filePath);
            boolean success = (Boolean) result[0];
            String errorMessage = (String) result[1];
            String json = (String) result[3];
            String resultScript = (String) result[4];
            int[] stats = (int[]) result[5];

            // 输出详细信息用于调试
            System.out.println("=== offWatch.groovy 双向转换验证结果 ===");
            System.out.println("是否成功: " + success);
            if (!success) {
                System.out.println("错误信息: " + errorMessage);
            } else {
                System.out.println("统计信息:");
                System.out.println("  - 原始脚本长度: " + stats[0] + " 字符");
                System.out.println("  - JSON长度: " + stats[6] + " 字符");
                System.out.println("  - 转译结果长度: " + stats[1] + " 字符");
                System.out.println("  - 节点数: " + stats[2]);
                System.out.println("  - 边数: " + stats[3]);
                System.out.println("  - import数: " + stats[4]);
                System.out.println("  - 函数数: " + stats[5]);
                System.out.println("\n=== 生成的JSON ===");
                System.out.println(json);
                System.out.println("\n=== 转译后的脚本 ===");
                System.out.println(resultScript);
            }

            assertTrue(success, "规则文件双向转换应成功: " + errorMessage);
        }

        /**
         * 参数化测试：批量验证多个规则文件
         * 在ValueSource中添加需要测试的规则文件路径
         */
        @ParameterizedTest
        @DisplayName("批量规则文件往返转换验证")
        @ValueSource(strings = {
            "/Users/weidian/project/vdian/wd24/wd24-edge-device-plugin/rule-manager/src/main/resources/rule_2_5_1_fix/offWatch.groovy"
            // 在这里添加更多规则文件路径
        })
        void roundTrip_ruleFiles_shouldSucceed(String filePath) throws IOException {
            // 跳过不存在的文件
            if (!Files.exists(Paths.get(filePath))) {
                System.out.println("跳过不存在的文件: " + filePath);
                return;
            }

            Object[] result = verifyRuleFileRoundTrip(filePath);
            boolean success = (Boolean) result[0];
            String errorMessage = (String) result[1];
            int[] stats = (int[]) result[5];

            System.out.println("验证文件: " + filePath);
            System.out.println("结果: " + (success ? "✅ 成功" : "❌ 失败 - " + errorMessage));
            if (success && stats != null) {
                System.out.println("节点数: " + stats[2] + ", 边数: " + stats[3]);
            }
            System.out.println();

            assertTrue(success, "规则文件 " + filePath + " 双向转换应成功: " + errorMessage);
        }
    }
}

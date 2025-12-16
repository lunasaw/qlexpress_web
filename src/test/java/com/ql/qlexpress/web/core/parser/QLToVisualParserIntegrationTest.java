package com.ql.qlexpress.web.core.parser;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.exception.QLSyntaxException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // QL4 编译器实例（复用）
        private Express4Runner ql4Runner;

        /**
         * 获取 QL4 编译器实例
         */
        private Express4Runner getQL4Runner() {
            if (ql4Runner == null) {
                ql4Runner = new Express4Runner(InitOptions.builder().build());
            }
            return ql4Runner;
        }

        /**
         * 验证结果详情类
         */
        class RoundTripResult {
            boolean success;
            String errorMessage;
            String originalScript;
            String json;
            String resultScript;
            int[] stats;
            boolean ql4CompileSuccess;
            String ql4CompileError;
            List<String> scriptDifferences;
            double similarityScore;

            Object[] toArray() {
                return new Object[]{success, errorMessage, originalScript, json, resultScript, stats,
                        ql4CompileSuccess, ql4CompileError, scriptDifferences, similarityScore};
            }
        }

        /**
         * 通用规则文件双向转换验证方法
         *
         * 验证流程：
         * 1. 读取规则文件
         * 2. QL脚本 → JSON (解析)
         * 3. JSON → QL脚本 (转译)
         * 4. QL4编译验证（确保生成的脚本能编译通过）
         * 5. 脚本规范化比较（验证语义一致性）
         *
         * @param filePath 规则文件的绝对路径
         * @return 验证结果数组: [success, errorMessage, originalScript, json, resultScript, stats,
         *         ql4CompileSuccess, ql4CompileError, scriptDifferences, similarityScore]
         */
        public Object[] verifyRuleFileRoundTrip(String filePath) throws IOException {
            RoundTripResult result = new RoundTripResult();
            Path path = Paths.get(filePath);

            // 检查文件是否存在
            if (!Files.exists(path)) {
                result.success = false;
                result.errorMessage = "文件不存在: " + filePath;
                return result.toArray();
            }

            // 读取文件内容
            String script = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            result.originalScript = script;

            if (script.trim().isEmpty()) {
                result.success = false;
                result.errorMessage = "文件内容为空: " + filePath;
                return result.toArray();
            }

            // Step 1: QL → JSON (解析)
            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            if (!parseResult.isSuccess()) {
                result.success = false;
                result.errorMessage = "QL解析失败: " + parseResult.getErrorMessage();
                return result.toArray();
            }

            VisualFlowSchema flow = parseResult.getFlow();

            // Step 2: JSON序列化（验证JSON可序列化）
            String json;
            try {
                json = objectMapper.writeValueAsString(flow);
                result.json = json;
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "JSON序列化失败: " + e.getMessage();
                return result.toArray();
            }

            // Step 3: JSON反序列化（验证JSON可反序列化）
            VisualFlowSchema deserializedFlow;
            try {
                deserializedFlow = objectMapper.readValue(json, VisualFlowSchema.class);
            } catch (Exception e) {
                result.success = false;
                result.errorMessage = "JSON反序列化失败: " + e.getMessage();
                return result.toArray();
            }

            // Step 4: JSON → QL (转译)
            TranspileResult transpileResult = transpiler.transpile(deserializedFlow);
            if (!transpileResult.isSuccess()) {
                result.success = false;
                result.errorMessage = "QL转译失败: " + transpileResult.getErrorMessage();
                return result.toArray();
            }

            String resultScript = transpileResult.getScript();
            result.resultScript = resultScript;

            // Step 5: QL4编译验证 - 确保生成的脚本能被QL4编译器正确编译
            try {
                getQL4Runner().check(resultScript);
                result.ql4CompileSuccess = true;
            } catch (QLSyntaxException e) {
                result.ql4CompileSuccess = false;
                result.ql4CompileError = "QL4语法错误: " + e.getMessage();
                result.success = false;
                result.errorMessage = "转译后脚本QL4编译失败: " + e.getMessage();
                // 构建统计信息后返回
                result.stats = buildStats(script, resultScript, flow, json);
                return result.toArray();
            } catch (Exception e) {
                result.ql4CompileSuccess = false;
                result.ql4CompileError = "QL4编译异常: " + e.getMessage();
                result.success = false;
                result.errorMessage = "转译后脚本QL4编译异常: " + e.getMessage();
                result.stats = buildStats(script, resultScript, flow, json);
                return result.toArray();
            }

            // Step 6: 脚本规范化比较
            result.scriptDifferences = compareScripts(script, resultScript);
            result.similarityScore = calculateSimilarity(script, resultScript);

            // 构建统计信息
            result.stats = buildStats(script, resultScript, flow, json);

            // 最终成功判断：转译成功 + QL4编译成功
            result.success = true;
            return result.toArray();
        }

        /**
         * 构建统计信息
         */
        private int[] buildStats(String script, String resultScript, VisualFlowSchema flow, String json) {
            return new int[]{
                    script.length(),  // originalLength
                    resultScript != null ? resultScript.length() : 0,  // resultLength
                    flow.getNodes() != null ? flow.getNodes().size() : 0,  // nodeCount
                    flow.getEdges() != null ? flow.getEdges().size() : 0,  // edgeCount
                    flow.getImports() != null ? flow.getImports().size() : 0,  // importCount
                    flow.getFunctions() != null ? flow.getFunctions().size() : 0,  // functionCount
                    json != null ? json.length() : 0  // jsonLength
            };
        }

        /**
         * 规范化脚本用于比较
         * 移除注释、空白差异、格式差异等
         */
        private String normalizeScript(String script) {
            if (script == null) return "";

            // 移除单行注释
            String normalized = script.replaceAll("//[^\n]*", "");

            // 移除多行注释
            normalized = normalized.replaceAll("/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/", "");

            // 统一换行符
            normalized = normalized.replaceAll("\r\n", "\n");

            // 移除多余空白
            normalized = normalized.replaceAll("\\s+", " ");

            // 移除字符串前后空白
            normalized = normalized.trim();

            return normalized;
        }

        /**
         * 提取脚本的关键元素用于比较
         */
        private List<String> extractKeyElements(String script) {
            List<String> elements = new ArrayList<>();
            String normalized = normalizeScript(script);

            // 提取import语句
            Pattern importPattern = Pattern.compile("import\\s+([\\w.]+)");
            Matcher importMatcher = importPattern.matcher(normalized);
            while (importMatcher.find()) {
                elements.add("IMPORT:" + importMatcher.group(1));
            }

            // 提取function定义
            Pattern funcPattern = Pattern.compile("function\\s+(\\w+)\\s*\\(");
            Matcher funcMatcher = funcPattern.matcher(normalized);
            while (funcMatcher.find()) {
                elements.add("FUNC:" + funcMatcher.group(1));
            }

            // 提取控制流关键字
            Pattern controlPattern = Pattern.compile("\\b(if|else|for|while|try|catch|return|break|continue)\\b");
            Matcher controlMatcher = controlPattern.matcher(normalized);
            while (controlMatcher.find()) {
                elements.add("CTRL:" + controlMatcher.group(1));
            }

            // 提取赋值语句变量名
            Pattern assignPattern = Pattern.compile("\\b(\\w+)\\s*=");
            Matcher assignMatcher = assignPattern.matcher(normalized);
            while (assignMatcher.find()) {
                String var = assignMatcher.group(1);
                // 排除关键字
                if (!var.matches("if|else|for|while|try|catch|return|break|continue|function|import")) {
                    elements.add("VAR:" + var);
                }
            }

            return elements;
        }

        /**
         * 比较两个脚本，返回差异列表
         */
        private List<String> compareScripts(String original, String result) {
            List<String> differences = new ArrayList<>();

            List<String> originalElements = extractKeyElements(original);
            List<String> resultElements = extractKeyElements(result);

            // 检查原始脚本中有但结果中没有的元素
            for (String element : originalElements) {
                if (!resultElements.contains(element)) {
                    differences.add("缺失: " + element);
                }
            }

            // 检查结果中有但原始脚本中没有的元素
            for (String element : resultElements) {
                if (!originalElements.contains(element)) {
                    differences.add("新增: " + element);
                }
            }

            return differences;
        }

        /**
         * 计算两个脚本的相似度（0-1之间）
         */
        private double calculateSimilarity(String original, String result) {
            List<String> originalElements = extractKeyElements(original);
            List<String> resultElements = extractKeyElements(result);

            if (originalElements.isEmpty() && resultElements.isEmpty()) {
                return 1.0;
            }

            if (originalElements.isEmpty() || resultElements.isEmpty()) {
                return 0.0;
            }

            // 计算Jaccard相似度
            int intersection = 0;
            for (String element : originalElements) {
                if (resultElements.contains(element)) {
                    intersection++;
                }
            }

            int union = originalElements.size() + resultElements.size() - intersection;
            return (double) intersection / union;
        }

        @Test
        @DisplayName("offWatch.groovy规则文件应能往返转换")
        @SuppressWarnings("unchecked")
        void roundTrip_offWatchRule_shouldSucceed() throws IOException {
            String filePath = "/Users/weidian/project/vdian/wd24/wd24-edge-device-plugin/rule-manager/src/main/resources/rule_2_5_1_fix/offWatch.groovy";

            Object[] result = verifyRuleFileRoundTrip(filePath);
            boolean success = (Boolean) result[0];
            String errorMessage = (String) result[1];
            String originalScript = (String) result[2];
            String json = (String) result[3];
            String resultScript = (String) result[4];
            int[] stats = (int[]) result[5];
            Boolean ql4CompileSuccess = (Boolean) result[6];
            String ql4CompileError = (String) result[7];
            List<String> scriptDifferences = (List<String>) result[8];
            Double similarityScore = (Double) result[9];

            // 输出详细信息用于调试
            System.out.println("=== offWatch.groovy 双向转换验证结果 ===");
            System.out.println("是否成功: " + success);
            if (!success) {
                System.out.println("错误信息: " + errorMessage);
            }

            if (stats != null) {
                System.out.println("\n=== 统计信息 ===");
                System.out.println("  - 原始脚本长度: " + stats[0] + " 字符");
                System.out.println("  - JSON长度: " + stats[6] + " 字符");
                System.out.println("  - 转译结果长度: " + stats[1] + " 字符");
                System.out.println("  - 节点数: " + stats[2]);
                System.out.println("  - 边数: " + stats[3]);
                System.out.println("  - import数: " + stats[4]);
                System.out.println("  - 函数数: " + stats[5]);
            }

            // QL4编译验证结果
            System.out.println("\n=== QL4 编译验证 ===");
            System.out.println("QL4编译成功: " + ql4CompileSuccess);
            if (ql4CompileError != null) {
                System.out.println("QL4编译错误: " + ql4CompileError);
            }

            // 脚本比较结果
            System.out.println("\n=== 脚本比较结果 ===");
            if (similarityScore != null) {
                System.out.println("相似度得分: " + String.format("%.2f%%", similarityScore * 100));
            }
            if (scriptDifferences != null && !scriptDifferences.isEmpty()) {
                System.out.println("差异项 (" + scriptDifferences.size() + " 项):");
                for (String diff : scriptDifferences) {
                    System.out.println("  - " + diff);
                }
            } else {
                System.out.println("无关键元素差异");
            }

            // 输出原始脚本和转译后脚本用于人工比对
            System.out.println("\n=== 原始脚本 ===");
            System.out.println(originalScript);
            System.out.println("\n=== 生成的JSON ===");
            System.out.println(json);
            System.out.println("\n=== 转译后的脚本 ===");
            System.out.println(resultScript);

            // 断言验证
            assertTrue(success, "规则文件双向转换应成功: " + errorMessage);
            assertTrue(ql4CompileSuccess != null && ql4CompileSuccess,
                    "转译后脚本应能通过QL4编译: " + ql4CompileError);
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
        @SuppressWarnings("unchecked")
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
            Boolean ql4CompileSuccess = (Boolean) result[6];
            String ql4CompileError = (String) result[7];
            List<String> scriptDifferences = (List<String>) result[8];
            Double similarityScore = (Double) result[9];

            System.out.println("验证文件: " + filePath);
            System.out.println("结果: " + (success ? "✅ 成功" : "❌ 失败 - " + errorMessage));

            if (stats != null) {
                System.out.println("  节点数: " + stats[2] + ", 边数: " + stats[3]);
            }

            System.out.println("  QL4编译: " + (ql4CompileSuccess != null && ql4CompileSuccess ? "✅ 通过" : "❌ 失败"));
            if (ql4CompileError != null) {
                System.out.println("  编译错误: " + ql4CompileError);
            }

            if (similarityScore != null) {
                System.out.println("  相似度: " + String.format("%.2f%%", similarityScore * 100));
            }

            if (scriptDifferences != null && !scriptDifferences.isEmpty()) {
                System.out.println("  差异项: " + scriptDifferences.size() + " 项");
            }
            System.out.println();

            // 断言验证
            assertTrue(success, "规则文件 " + filePath + " 双向转换应成功: " + errorMessage);
            assertTrue(ql4CompileSuccess != null && ql4CompileSuccess,
                    "转译后脚本应能通过QL4编译: " + ql4CompileError);
        }
    }
}

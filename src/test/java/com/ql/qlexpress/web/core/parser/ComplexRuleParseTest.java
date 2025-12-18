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
 * 复杂规则解析测试
 * 模拟 blueTooth.groovy 类型的复杂业务规则
 *
 * @author qlexpress
 */
class ComplexRuleParseTest {

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
    @DisplayName("Import和类型转换测试")
    class ImportAndCastTest {

        @Test
        @DisplayName("多import语句应能正确解析")
        void parse_multipleImports_shouldParseCorrectly() {
            String script = "import java.util.List;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.ArrayList;\n" +
                    "import java.util.HashMap;\n" +
                    "import java.math.BigDecimal;\n" +
                    "x = 1;";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
            assertEquals(5, result.getFlow().getImports().size(), "应有5个import");
        }

        @Test
        @DisplayName("类型转换表达式应能正确解析")
        void parse_castExpression_shouldParseCorrectly() {
            String script = "Object obj = \"hello\";\n" +
                    "String str = (String) obj;\n" +
                    "int num = (int) value;\n" +
                    "List list = (List) collection;";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("函数定义测试")
    class FunctionDefinitionTest {

        @Test
        @DisplayName("简单函数定义应能正确解析")
        void parse_simpleFunctionDefinition_shouldParseCorrectly() {
            String script = "function add(int a, int b) {\n" +
                    "    return a + b;\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
            assertFalse(result.getFlow().getFunctions().isEmpty(), "应有函数定义");
            assertEquals("add", result.getFlow().getFunctions().get(0).getName());
        }

        @Test
        @DisplayName("带参数的函数定义应能正确解析")
        void parse_typedFunctionDefinition_shouldParseCorrectly() {
            // QLExpress4函数定义语法: function name(params) { body }
            String script = "function formatName(String firstName, String lastName) {\n" +
                    "    return firstName + \" \" + lastName;\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("复杂函数体应能正确解析")
        void parse_complexFunctionBody_shouldParseCorrectly() {
            // QLExpress4函数定义语法不支持返回类型
            String script = "function factorial(int n) {\n" +
                    "    if (n <= 1) {\n" +
                    "        return 1;\n" +
                    "    }\n" +
                    "    return n * factorial(n - 1);\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("复杂条件分支测试")
    class ComplexConditionTest {

        @Test
        @DisplayName("多层嵌套if应能正确解析")
        void parse_nestedIf_shouldParseCorrectly() {
            String script = "if (level == 1) {\n" +
                    "    if (score > 80) {\n" +
                    "        result = \"优秀\";\n" +
                    "    } else {\n" +
                    "        result = \"普通\";\n" +
                    "    }\n" +
                    "} else if (level == 2) {\n" +
                    "    result = \"高级\";\n" +
                    "} else {\n" +
                    "    result = \"未知\";\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");
            assertTrue(transpileResult.getScript().contains("if"), "应包含if");
            assertTrue(transpileResult.getScript().contains("else"), "应包含else");
        }

        @Test
        @DisplayName("复合条件表达式应能正确解析")
        void parse_compoundCondition_shouldParseCorrectly() {
            String script = "if (age >= 18 && age <= 60 && status == \"active\") {\n" +
                    "    eligible = true;\n" +
                    "}\n" +
                    "if (type == \"A\" || type == \"B\" || (type == \"C\" && level > 5)) {\n" +
                    "    approved = true;\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("循环语句测试")
    class LoopStatementTest {

        @Test
        @DisplayName("嵌套for循环应能正确解析")
        void parse_nestedForLoop_shouldParseCorrectly() {
            String script = "for (i = 0; i < rows; i = i + 1) {\n" +
                    "    for (j = 0; j < cols; j = j + 1) {\n" +
                    "        matrix[i][j] = i * cols + j;\n" +
                    "    }\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("foreach嵌套循环应能正确解析")
        void parse_nestedForeach_shouldParseCorrectly() {
            String script = "for (Map item : items) {\n" +
                    "    for (String key : item.keySet()) {\n" +
                    "        println(key + \": \" + item.get(key));\n" +
                    "    }\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("带break和continue的循环应能正确解析")
        void parse_loopWithBreakContinue_shouldParseCorrectly() {
            String script = "for (i = 0; i < 100; i = i + 1) {\n" +
                    "    if (i % 2 == 0) {\n" +
                    "        continue;\n" +
                    "    }\n" +
                    "    if (i > 50) {\n" +
                    "        break;\n" +
                    "    }\n" +
                    "    sum = sum + i;\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");
            assertTrue(transpileResult.getScript().contains("break"), "应包含break");
            assertTrue(transpileResult.getScript().contains("continue"), "应包含continue");
        }
    }

    @Nested
    @DisplayName("异常处理测试")
    class ExceptionHandlingTest {

        @Test
        @DisplayName("try-catch-finally应能正确解析")
        void parse_tryCatchFinally_shouldParseCorrectly() {
            String script = "try {\n" +
                    "    result = riskyOperation();\n" +
                    "} catch (Exception e) {\n" +
                    "    result = defaultValue;\n" +
                    "    logError(e);\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");
            assertTrue(transpileResult.getScript().contains("try"), "应包含try");
            assertTrue(transpileResult.getScript().contains("catch"), "应包含catch");
        }

        @Test
        @DisplayName("throw语句应能正确解析")
        void parse_throwStatement_shouldParseCorrectly() {
            String script = "if (value < 0) {\n" +
                    "    throw new IllegalArgumentException(\"值不能为负数\");\n" +
                    "}";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");
            assertTrue(transpileResult.getScript().contains("throw"), "应包含throw");
        }
    }

    @Nested
    @DisplayName("方法调用链测试")
    class MethodChainTest {

        @Test
        @DisplayName("链式方法调用应能正确解析")
        void parse_chainedMethodCall_shouldParseCorrectly() {
            String script = "result = str.trim().toLowerCase().replace(\"-\", \"_\").split(\"_\");";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("静态方法调用应能正确解析")
        void parse_staticMethodCall_shouldParseCorrectly() {
            String script = "value = Math.abs(-10);\n" +
                    "maxVal = Math.max(a, b);\n" +
                    "formatted = String.format(\"%s: %d\", name, count);";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("new表达式测试")
    class NewExpressionTest {

        @Test
        @DisplayName("各种new表达式应能正确解析")
        void parse_newExpressions_shouldParseCorrectly() {
            String script = "list = new ArrayList();\n" +
                    "map = new HashMap();\n" +
                    "sb = new StringBuilder(\"hello\");\n" +
                    "bd = new BigDecimal(\"123.456\");\n" +
                    "ex = new RuntimeException(\"error message\");";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }

        @Test
        @DisplayName("带泛型的new表达式应能正确解析")
        void parse_newWithGenerics_shouldParseCorrectly() {
            String script = "List list = new ArrayList();\n" +
                    "Map map = new HashMap();";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("复杂业务规则模拟测试")
    class ComplexBusinessRuleTest {

        @Test
        @DisplayName("蓝牙设备规则模拟 - 设备验证")
        void parse_bluetoothDeviceValidation_shouldParseCorrectly() {
            // QLExpress4函数定义语法不支持返回类型
            String script = "import java.util.List;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.ArrayList;\n" +
                    "\n" +
                    "function validateDevice(Map device) {\n" +
                    "    if (device == null) {\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "    String name = (String) device.get(\"name\");\n" +
                    "    if (name == null || name.trim().length() == 0) {\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "    Integer rssi = (Integer) device.get(\"rssi\");\n" +
                    "    if (rssi == null || rssi < -100) {\n" +
                    "        return false;\n" +
                    "    }\n" +
                    "    return true;\n" +
                    "}\n" +
                    "\n" +
                    "// 验证设备列表\n" +
                    "List validDevices = new ArrayList();\n" +
                    "for (Map device : devices) {\n" +
                    "    if (validateDevice(device)) {\n" +
                    "        validDevices.add(device);\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "return validDevices;";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());
            assertEquals(3, result.getFlow().getImports().size(), "应有3个import");
            assertFalse(result.getFlow().getFunctions().isEmpty(), "应有函数定义");
        }

        @Test
        @DisplayName("蓝牙设备规则模拟 - 设备过滤")
        void parse_bluetoothDeviceFiltering_shouldParseCorrectly() {
            String script = "import java.util.List;\n" +
                    "import java.util.ArrayList;\n" +
                    "\n" +
                    "// 设备过滤配置\n" +
                    "int minRssi = -70;\n" +
                    "String requiredPrefix = \"BT_\";\n" +
                    "\n" +
                    "// 过滤设备\n" +
                    "List filteredDevices = new ArrayList();\n" +
                    "\n" +
                    "for (Map device : devices) {\n" +
                    "    String name = (String) device.get(\"name\");\n" +
                    "    Integer rssi = (Integer) device.get(\"rssi\");\n" +
                    "\n" +
                    "    // 信号强度过滤\n" +
                    "    if (rssi < minRssi) {\n" +
                    "        continue;\n" +
                    "    }\n" +
                    "\n" +
                    "    // 名称前缀过滤\n" +
                    "    if (name != null && name.startsWith(requiredPrefix)) {\n" +
                    "        // 添加��外信息\n" +
                    "        device.put(\"filtered\", true);\n" +
                    "        device.put(\"filterTime\", System.currentTimeMillis());\n" +
                    "        filteredDevices.add(device);\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "// 返回结果\n" +
                    "return filteredDevices;";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            // 验证往返转换
            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String outputScript = transpileResult.getScript();
            assertTrue(outputScript.contains("for"), "应包含for循环");
            assertTrue(outputScript.contains("if"), "应包含if条件");
            assertTrue(outputScript.contains("continue"), "应包含continue");
        }

        @Test
        @DisplayName("蓝牙设备规则模拟 - 设备配对")
        void parse_bluetoothDevicePairing_shouldParseCorrectly() {
            // QLExpress4函数定义语法不支持返回类型
            String script = "import java.util.Map;\n" +
                    "import java.util.HashMap;\n" +
                    "\n" +
                    "function pairDevice(Map device) {\n" +
                    "    Map result = new HashMap();\n" +
                    "\n" +
                    "    try {\n" +
                    "        String address = (String) device.get(\"address\");\n" +
                    "        if (address == null) {\n" +
                    "            throw new IllegalArgumentException(\"设备地址不能为空\");\n" +
                    "        }\n" +
                    "\n" +
                    "        // 模拟配对过程\n" +
                    "        boolean success = true;\n" +
                    "\n" +
                    "        if (success) {\n" +
                    "            result.put(\"status\", \"paired\");\n" +
                    "            result.put(\"address\", address);\n" +
                    "            result.put(\"pairTime\", System.currentTimeMillis());\n" +
                    "        }\n" +
                    "\n" +
                    "    } catch (Exception e) {\n" +
                    "        result.put(\"status\", \"error\");\n" +
                    "        result.put(\"error\", e.getMessage());\n" +
                    "    }\n" +
                    "\n" +
                    "    return result;\n" +
                    "}\n" +
                    "\n" +
                    "// 执行配对\n" +
                    "pairResult = pairDevice(targetDevice);\n" +
                    "return pairResult;";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            // 验证函数定义
            assertFalse(result.getFlow().getFunctions().isEmpty(), "应有函数定义");
            assertEquals("pairDevice", result.getFlow().getFunctions().get(0).getName());

            // 验证往返转换
            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");
        }

        @Test
        @DisplayName("完整业务规则 - 订单处理")
        void parse_orderProcessingRule_shouldParseCorrectly() {
            String script = "import java.util.List;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.ArrayList;\n" +
                    "import java.math.BigDecimal;\n" +
                    "\n" +
                    "// 计算订单总价\n" +
                    "function calculateTotal(List items) {\n" +
                    "    BigDecimal total = new BigDecimal(\"0\");\n" +
                    "\n" +
                    "    for (Map item : items) {\n" +
                    "        BigDecimal price = (BigDecimal) item.get(\"price\");\n" +
                    "        Integer quantity = (Integer) item.get(\"quantity\");\n" +
                    "\n" +
                    "        if (price != null && quantity != null) {\n" +
                    "            BigDecimal itemTotal = price.multiply(new BigDecimal(quantity));\n" +
                    "            total = total.add(itemTotal);\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "    return total;\n" +
                    "}\n" +
                    "\n" +
                    "// 应用折扣\n" +
                    "function applyDiscount(BigDecimal total, String discountType) {\n" +
                    "    BigDecimal discount = new BigDecimal(\"0\");\n" +
                    "\n" +
                    "    if (discountType == \"VIP\") {\n" +
                    "        discount = total.multiply(new BigDecimal(\"0.1\"));\n" +
                    "    } else if (discountType == \"MEMBER\") {\n" +
                    "        discount = total.multiply(new BigDecimal(\"0.05\"));\n" +
                    "    } else if (discountType == \"NEW\") {\n" +
                    "        discount = total.multiply(new BigDecimal(\"0.15\"));\n" +
                    "    }\n" +
                    "\n" +
                    "    return total.subtract(discount);\n" +
                    "}\n" +
                    "\n" +
                    "// 主��理逻辑\n" +
                    "BigDecimal orderTotal = calculateTotal(orderItems);\n" +
                    "BigDecimal finalPrice = applyDiscount(orderTotal, customerType);\n" +
                    "\n" +
                    "// 检查最低消费\n" +
                    "if (finalPrice.compareTo(new BigDecimal(\"10\")) < 0) {\n" +
                    "    finalPrice = new BigDecimal(\"10\");\n" +
                    "}\n" +
                    "\n" +
                    "return finalPrice;";

            QLToVisualParser.ParseResult result = parser.parse(script);
            assertTrue(result.isSuccess(), "解析应成功: " + result.getErrorMessage());

            // 验证import
            assertEquals(4, result.getFlow().getImports().size(), "应有4个import");

            // 验证函数
            assertEquals(2, result.getFlow().getFunctions().size(), "应有2个函数定义");

            // 验证往返转换
            TranspileResult transpileResult = transpiler.transpile(result.getFlow());
            assertTrue(transpileResult.isSuccess(), "转译应成功");

            String outputScript = transpileResult.getScript();
            assertTrue(outputScript.contains("import java.util.List"), "应包含List import");
            assertTrue(outputScript.contains("function"), "应包含function定义");
        }
    }

    @Nested
    @DisplayName("JSON序列化往返测试")
    class JsonRoundTripTest {

        @Test
        @DisplayName("复杂规则的JSON序列化应保持一致性")
        void jsonRoundTrip_complexRule_shouldPreserveStructure() throws Exception {
            String script = "import java.util.List;\n" +
                    "\n" +
                    "function process(int x) {\n" +
                    "    if (x > 0) {\n" +
                    "        return x * 2;\n" +
                    "    } else {\n" +
                    "        return 0;\n" +
                    "    }\n" +
                    "}\n" +
                    "\n" +
                    "result = process(input);\n" +
                    "return result;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功");

            VisualFlowSchema original = parseResult.getFlow();

            // 序列化
            String json = objectMapper.writeValueAsString(original);
            assertNotNull(json);

            // 反序列化
            VisualFlowSchema deserialized = objectMapper.readValue(json, VisualFlowSchema.class);
            assertNotNull(deserialized);

            // 验证结构一致性
            assertEquals(original.getImports().size(), deserialized.getImports().size());
            assertEquals(original.getFunctions().size(), deserialized.getFunctions().size());
            assertEquals(original.getNodes().size(), deserialized.getNodes().size());
            assertEquals(original.getEdges().size(), deserialized.getEdges().size());

            // 验证转译结果一致性
            TranspileResult result1 = transpiler.transpile(original);
            TranspileResult result2 = transpiler.transpile(deserialized);

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
            assertEquals(result1.getScript(), result2.getScript());
        }
    }
}

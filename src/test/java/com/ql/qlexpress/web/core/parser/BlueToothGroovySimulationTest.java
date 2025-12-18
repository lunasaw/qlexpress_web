package com.ql.qlexpress.web.core.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ql.qlexpress.web.core.transpiler.TranspileResult;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import com.ql.qlexpress.web.model.visual.VisualNode;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * blueTooth.groovy 模拟测试
 *
 * 验证完整的QL↔JSON双向转换功能，模拟生产环境中的蓝牙开门规则场景。
 * 测试覆盖：
 * - import语句
 * - 函数定义（带复杂函数体）
 * - 类型转换
 * - new表达式
 * - 方法调用链
 * - if/else if/else多分支
 * - for/foreach循环
 * - try-catch-finally
 * - break/continue
 * - throw异常
 * - 嵌套结构
 *
 * @author qlexpress
 */
class BlueToothGroovySimulationTest {

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
    @DisplayName("完整蓝牙开门规则模拟测试")
    class FullBluetoothRuleTest {

        @Test
        @DisplayName("蓝牙开门主流程 - 认证与控制")
        void fullBluetoothDoorRule_shouldParseAndTranspileCorrectly() throws Exception {
            // 模拟完整的蓝牙开门规则
            String script =
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.HashMap;\n" +
                "import com.alibaba.fastjson.JSON;\n" +
                "import com.alibaba.fastjson.JSONArray;\n" +
                "\n" +
                "// 设备验证函数\n" +
                "function validateDevice(Map device) {\n" +
                "    if (device == null) {\n" +
                "        return false;\n" +
                "    }\n" +
                "    String address = (String) device.get(\"address\");\n" +
                "    if (address == null || address.length() == 0) {\n" +
                "        return false;\n" +
                "    }\n" +
                "    Integer rssi = (Integer) device.get(\"rssi\");\n" +
                "    if (rssi == null || rssi < -100) {\n" +
                "        return false;\n" +
                "    }\n" +
                "    return true;\n" +
                "}\n" +
                "\n" +
                "// 执行开门函数\n" +
                "function openDoor(Map doorService, String doorNo) {\n" +
                "    Map result = new HashMap();\n" +
                "    try {\n" +
                "        Map resp = doorService.onCommand(\"open\", doorNo);\n" +
                "        if (resp.get(\"success\") == true) {\n" +
                "            result.put(\"status\", \"success\");\n" +
                "            result.put(\"doorNo\", doorNo);\n" +
                "        } else {\n" +
                "            result.put(\"status\", \"failed\");\n" +
                "            result.put(\"error\", resp.get(\"message\"));\n" +
                "        }\n" +
                "    } catch (Exception e) {\n" +
                "        result.put(\"status\", \"error\");\n" +
                "        result.put(\"error\", e.getMessage());\n" +
                "    }\n" +
                "    return result;\n" +
                "}\n" +
                "\n" +
                "// 主控制逻辑\n" +
                "aBCMStr = JSON.toJSONString(appBluetoothCommandModel);\n" +
                "connectedBleStr = JSON.toJSONString(authedBluetoothModel);\n" +
                "\n" +
                "type = temp.getType();\n" +
                "List results = new ArrayList();\n" +
                "\n" +
                "// 根据类型处理不同命令\n" +
                "if (type == 1) {\n" +
                "    // 认证流程\n" +
                "    if (validateDevice(deviceInfo)) {\n" +
                "        authResult = authenticate(deviceInfo);\n" +
                "        results.add(authResult);\n" +
                "    } else {\n" +
                "        throw new IllegalArgumentException(\"设备验证失败\");\n" +
                "    }\n" +
                "} else if (type == 2) {\n" +
                "    // 开门流程\n" +
                "    for (Map door : doorList) {\n" +
                "        String doorNo = (String) door.get(\"doorNo\");\n" +
                "        if (doorNo == null) {\n" +
                "            continue;\n" +
                "        }\n" +
                "        doorResult = openDoor(doorService, doorNo);\n" +
                "        results.add(doorResult);\n" +
                "        if (doorResult.get(\"status\") == \"error\") {\n" +
                "            break;\n" +
                "        }\n" +
                "    }\n" +
                "} else if (type == 3) {\n" +
                "    // 状态查询\n" +
                "    for (i = 0; i < doorServiceList.size(); i = i + 1) {\n" +
                "        svc = doorServiceList.get(i);\n" +
                "        statusResult = svc.getStatus();\n" +
                "        results.add(statusResult);\n" +
                "    }\n" +
                "} else {\n" +
                "    throw new IllegalArgumentException(\"未知的命令类型: \" + type);\n" +
                "}\n" +
                "\n" +
                "// 返回结果\n" +
                "response.setResults(results);\n" +
                "return response;";

            // 1. 解析
            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            VisualFlowSchema flow = parseResult.getFlow();
            assertNotNull(flow);

            // 验证import数量
            assertEquals(6, flow.getImports().size(), "应有6个import");
            assertTrue(flow.getImports().contains("java.util.List"));
            assertTrue(flow.getImports().contains("com.alibaba.fastjson.JSON"));

            // 验证函数定义
            assertEquals(2, flow.getFunctions().size(), "应有2个函数定义");
            List<String> funcNames = flow.getFunctions().stream()
                    .map(f -> f.getName())
                    .collect(Collectors.toList());
            assertTrue(funcNames.contains("validateDevice"), "应包含validateDevice函数");
            assertTrue(funcNames.contains("openDoor"), "应包含openDoor函数");

            // 验证节点类型
            assertTrue(hasNodeOfType(flow.getNodes(), "if"), "应有if节点");
            assertTrue(hasNodeOfType(flow.getNodes(), "for"), "应有for节点");
            assertTrue(hasNodeOfType(flow.getNodes(), "foreach"), "应有foreach节点");
            assertTrue(hasNodeOfType(flow.getNodes(), "throw"), "应有throw节点");

            // 2. JSON序列化往返
            String json = objectMapper.writeValueAsString(flow);
            assertNotNull(json);
            assertTrue(json.contains("\"type\" : \"if\""));
            assertTrue(json.contains("\"type\" : \"for\""));

            VisualFlowSchema deserializedFlow = objectMapper.readValue(json, VisualFlowSchema.class);
            assertNotNull(deserializedFlow);
            assertEquals(flow.getImports().size(), deserializedFlow.getImports().size());
            assertEquals(flow.getFunctions().size(), deserializedFlow.getFunctions().size());
            assertEquals(flow.getNodes().size(), deserializedFlow.getNodes().size());
            assertEquals(flow.getEdges().size(), deserializedFlow.getEdges().size());

            // 3. 转译回QL脚本
            TranspileResult transpileResult = transpiler.transpile(flow);
            assertTrue(transpileResult.isSuccess(), "转译应成功: " + transpileResult.getErrorMessage());

            String outputScript = transpileResult.getScript();
            assertNotNull(outputScript);

            // 验证转译输出包含关键元素
            assertTrue(outputScript.contains("import java.util.List"), "输出应包含import");
            assertTrue(outputScript.contains("function validateDevice"), "输出应包含validateDevice函数");
            assertTrue(outputScript.contains("function openDoor"), "输出应包含openDoor函数");
            assertTrue(outputScript.contains("if ("), "输出应包含if语句");
            assertTrue(outputScript.contains("else if"), "输出应包含else if");
            assertTrue(outputScript.contains("} else {"), "输出应包含else");
            assertTrue(outputScript.contains("for ("), "输出应包含for循环");
            assertTrue(outputScript.contains("try {"), "输出应包含try");
            assertTrue(outputScript.contains("catch ("), "输出应包含catch");
            assertTrue(outputScript.contains("break;"), "输出应包含break");
            assertTrue(outputScript.contains("continue;"), "输出应包含continue");
            assertTrue(outputScript.contains("throw "), "输出应包含throw");

            System.out.println("=== 原始脚本 ===");
            System.out.println(script);
            System.out.println("\n=== 转译输出 ===");
            System.out.println(outputScript);
        }

        @Test
        @DisplayName("蓝牙设备状态报告规则")
        void bluetoothStatusReportRule_shouldWorkCorrectly() throws Exception {
            String script =
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "import java.util.ArrayList;\n" +
                "import com.alibaba.fastjson.JSONArray;\n" +
                "\n" +
                "// 构建状态报告\n" +
                "function buildStatusReport(List doors) {\n" +
                "    JSONArray jsonArray = new JSONArray();\n" +
                "    for (Map door : doors) {\n" +
                "        String status = (String) door.get(\"status\");\n" +
                "        if (status == \"OPEN\") {\n" +
                "            door.put(\"statusCode\", 1);\n" +
                "        } else if (status == \"CLOSE\") {\n" +
                "            door.put(\"statusCode\", 0);\n" +
                "        } else {\n" +
                "            door.put(\"statusCode\", -1);\n" +
                "        }\n" +
                "        jsonArray.add(door);\n" +
                "    }\n" +
                "    return jsonArray;\n" +
                "}\n" +
                "\n" +
                "// 主逻辑\n" +
                "gmsConnected = component.getGlobalValue(GMS_CONNECTED);\n" +
                "\n" +
                "if (gmsConnected) {\n" +
                "    statusReport = buildStatusReport(doorStatusList);\n" +
                "    AckAdapter.eventReport(type, gmsModel, statusReport);\n" +
                "    result = true;\n" +
                "} else {\n" +
                "    result = false;\n" +
                "}\n" +
                "\n" +
                "return result;";

            // 解析
            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            // 验证函数
            assertEquals(1, parseResult.getFlow().getFunctions().size());
            assertEquals("buildStatusReport", parseResult.getFlow().getFunctions().get(0).getName());

            // 转译
            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess());

            String output = transpileResult.getScript();
            assertTrue(output.contains("else if"), "应包含else if");
            assertTrue(output.contains("} else {"), "应包含else");
            assertTrue(output.contains("for ("), "应包含for循环");

            System.out.println("=== 状态报告规则转译输出 ===");
            System.out.println(output);
        }

        @Test
        @DisplayName("蓝牙喇叭播报规则")
        void bluetoothHornAnnouncementRule_shouldWorkCorrectly() throws Exception {
            String script =
                "import java.util.List;\n" +
                "import java.util.Map;\n" +
                "\n" +
                "// 播放音频\n" +
                "function playAudio(Map hornService, String audioId) {\n" +
                "    if (hornService == null) {\n" +
                "        return false;\n" +
                "    }\n" +
                "    try {\n" +
                "        hornService.onCommand(\"play\", audioId);\n" +
                "        return true;\n" +
                "    } catch (Exception e) {\n" +
                "        logError(\"播放失败: \" + e.getMessage());\n" +
                "        return false;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// 查找喇叭服务\n" +
                "innerHornService = null;\n" +
                "outerHornService = null;\n" +
                "\n" +
                "for (Map service : hornServiceList) {\n" +
                "    String location = (String) service.get(\"location\");\n" +
                "    if (location == \"INNER\") {\n" +
                "        innerHornService = service;\n" +
                "    } else if (location == \"OUTER\") {\n" +
                "        outerHornService = service;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "// 根据场景播放\n" +
                "if (playScene == \"WELCOME\") {\n" +
                "    playAudio(innerHornService, welcomeAudio);\n" +
                "    playAudio(outerHornService, welcomeAudio);\n" +
                "} else if (playScene == \"DOOR_OPEN\") {\n" +
                "    playAudio(innerHornService, doorOpenAudio);\n" +
                "} else if (playScene == \"ERROR\") {\n" +
                "    playAudio(outerHornService, errorAudio);\n" +
                "}\n" +
                "\n" +
                "return true;";

            // 解析
            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            // 验证
            assertEquals(1, parseResult.getFlow().getFunctions().size());
            assertTrue(hasNodeOfType(parseResult.getFlow().getNodes(), "foreach"));
            assertTrue(hasNodeOfType(parseResult.getFlow().getNodes(), "if"));

            // 转译
            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess());

            String output = transpileResult.getScript();
            assertTrue(output.contains("try {"));
            assertTrue(output.contains("catch ("));
            assertTrue(output.contains("else if"));

            System.out.println("=== 喇叭播报规则转译输出 ===");
            System.out.println(output);
        }
    }

    @Nested
    @DisplayName("边界情况测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("深度嵌套结构应能正确处理")
        void deeplyNestedStructure_shouldWork() throws Exception {
            String script =
                "for (i = 0; i < 10; i = i + 1) {\n" +
                "    for (j = 0; j < 10; j = j + 1) {\n" +
                "        if (i > j) {\n" +
                "            if (i % 2 == 0) {\n" +
                "                if (j % 2 == 0) {\n" +
                "                    sum = sum + i * j;\n" +
                "                } else {\n" +
                "                    sum = sum + i;\n" +
                "                }\n" +
                "            }\n" +
                "        } else {\n" +
                "            if (i == j) {\n" +
                "                continue;\n" +
                "            }\n" +
                "            sum = sum + j;\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "return sum;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess());

            String output = transpileResult.getScript();
            assertTrue(output.contains("for ("));
            assertTrue(output.contains("if ("));
            assertTrue(output.contains("else {"));
            assertTrue(output.contains("continue;"));
        }

        @Test
        @DisplayName("多个try-catch块应能正确处理")
        void multipleTryCatchBlocks_shouldWork() throws Exception {
            String script =
                "result1 = null;\n" +
                "result2 = null;\n" +
                "\n" +
                "try {\n" +
                "    result1 = operation1();\n" +
                "} catch (Exception e1) {\n" +
                "    result1 = default1;\n" +
                "}\n" +
                "\n" +
                "try {\n" +
                "    result2 = operation2();\n" +
                "} catch (Exception e2) {\n" +
                "    result2 = default2;\n" +
                "}\n" +
                "\n" +
                "return result1 + result2;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess());

            long tryCatchCount = parseResult.getFlow().getNodes().stream()
                    .filter(n -> "try_catch".equals(n.getType()))
                    .count();
            assertEquals(2, tryCatchCount, "应有2个try_catch节点");

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess());

            String output = transpileResult.getScript();
            // 统计try出现次数
            int tryCount = countOccurrences(output, "try {");
            assertEquals(2, tryCount, "输出应有2个try块");
        }

        @Test
        @DisplayName("复杂表达式应能正确处理")
        void complexExpressions_shouldWork() throws Exception {
            String script =
                "import java.math.BigDecimal;\n" +
                "\n" +
                "// 复杂表达式测试\n" +
                "a = obj.getMethod1().getMethod2().getValue();\n" +
                "b = (String) map.get(\"key\").trim().toLowerCase();\n" +
                "c = new BigDecimal(\"123.456\").multiply(new BigDecimal(\"2\"));\n" +
                "d = list.get(0).toString().length();\n" +
                "e = Math.max(Math.min(a, b), c);\n" +
                "\n" +
                "if (a > 0 && b != null && c.compareTo(BigDecimal.ZERO) > 0) {\n" +
                "    result = a + b + c;\n" +
                "}\n" +
                "\n" +
                "return result;";

            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess(), "解析应成功: " + parseResult.getErrorMessage());

            TranspileResult transpileResult = transpiler.transpile(parseResult.getFlow());
            assertTrue(transpileResult.isSuccess());

            String output = transpileResult.getScript();
            assertTrue(output.contains("import java.math.BigDecimal"));
            assertTrue(output.contains("new BigDecimal"));
        }
    }

    @Nested
    @DisplayName("往返一致性测试")
    class RoundTripConsistencyTest {

        @Test
        @DisplayName("QL→JSON→QL往返应保持语义一致")
        void roundTrip_shouldPreserveSemantics() throws Exception {
            String originalScript =
                "import java.util.List;\n" +
                "\n" +
                "function calculate(int x) {\n" +
                "    if (x > 0) {\n" +
                "        return x * 2;\n" +
                "    } else {\n" +
                "        return 0;\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "sum = 0;\n" +
                "for (i = 0; i < 10; i = i + 1) {\n" +
                "    if (i % 2 == 0) {\n" +
                "        continue;\n" +
                "    }\n" +
                "    sum = sum + calculate(i);\n" +
                "}\n" +
                "\n" +
                "return sum;";

            // 第一次解析
            QLToVisualParser.ParseResult parseResult1 = parser.parse(originalScript);
            assertTrue(parseResult1.isSuccess());

            // 转译
            TranspileResult transpileResult1 = transpiler.transpile(parseResult1.getFlow());
            assertTrue(transpileResult1.isSuccess());
            String script1 = transpileResult1.getScript();

            // 第二次解析（用转译结果）
            QLToVisualParser.ParseResult parseResult2 = parser.parse(script1);
            assertTrue(parseResult2.isSuccess(), "第二次解析应成功: " + parseResult2.getErrorMessage());

            // 第二次转译
            TranspileResult transpileResult2 = transpiler.transpile(parseResult2.getFlow());
            assertTrue(transpileResult2.isSuccess());
            String script2 = transpileResult2.getScript();

            // 比较两次转译结果（应该相同或语义等价）
            assertEquals(script1, script2, "两次转译结果应该一致");

            System.out.println("=== 往返测试 ===");
            System.out.println("原始脚本:");
            System.out.println(originalScript);
            System.out.println("\n第一次转译:");
            System.out.println(script1);
            System.out.println("\n第二次转译:");
            System.out.println(script2);
        }

        @Test
        @DisplayName("JSON序列化往返应保持结构一致")
        void jsonRoundTrip_shouldPreserveStructure() throws Exception {
            String script =
                "import java.util.Map;\n" +
                "\n" +
                "function process(Map data) {\n" +
                "    if (data == null) {\n" +
                "        return null;\n" +
                "    }\n" +
                "    return data.get(\"value\");\n" +
                "}\n" +
                "\n" +
                "for (item : items) {\n" +
                "    result = process(item);\n" +
                "    if (result != null) {\n" +
                "        output.add(result);\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "return output;";

            // 解析
            QLToVisualParser.ParseResult parseResult = parser.parse(script);
            assertTrue(parseResult.isSuccess());
            VisualFlowSchema original = parseResult.getFlow();

            // JSON往返
            String json = objectMapper.writeValueAsString(original);
            VisualFlowSchema deserialized = objectMapper.readValue(json, VisualFlowSchema.class);

            // 比较
            assertEquals(original.getImports().size(), deserialized.getImports().size());
            assertEquals(original.getFunctions().size(), deserialized.getFunctions().size());
            assertEquals(original.getNodes().size(), deserialized.getNodes().size());
            assertEquals(original.getEdges().size(), deserialized.getEdges().size());

            // 比较转译结果
            TranspileResult result1 = transpiler.transpile(original);
            TranspileResult result2 = transpiler.transpile(deserialized);

            assertEquals(result1.getScript(), result2.getScript(), "JSON往返后转译结果应一致");
        }
    }

    // ==================== 辅助方法 ====================

    private boolean hasNodeOfType(List<VisualNode> nodes, String type) {
        if (nodes == null) return false;
        return nodes.stream().anyMatch(n -> type.equals(n.getType()));
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}

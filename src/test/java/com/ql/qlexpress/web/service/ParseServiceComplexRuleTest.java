package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.parser.QLToVisualParser;
import com.ql.qlexpress.web.model.dto.ParseToVisualRequest;
import com.ql.qlexpress.web.model.dto.ParseToVisualResponse;
import com.ql.qlexpress.web.model.visual.VisualNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ParseService 复杂业务规则测试 - 测试类似蓝牙开门规则的复杂场景
 *
 * 这些测试用例基于实际生产环境中的复杂规则简化而来，
 * 用于验证解析器对复杂业务逻辑的处理能力。
 *
 * @author qlexpress
 */
class ParseServiceComplexRuleTest {

    private ParseService parseService;

    @BeforeEach
    void setUp() {
        QLToVisualParser parser = new QLToVisualParser();
        parseService = new ParseService(parser);
    }

    @Nested
    @DisplayName("蓝牙开门规则简化版测试")
    class BluetoothRuleTest {

        @Test
        @DisplayName("蓝牙开门规则简化版应能解析 - 类型判断分支")
        void parseToVisual_simplifiedBluetoothRule_shouldParse() {
            // 简化版蓝牙规则 - 当前模型能支持的部分
            String script = "aBCMStr = JSON.toJSONString(appBluetoothCommandModel);\n" +
                    "connectedBleStr = JSON.toJSONString(authedBluetoothModel);\n" +
                    "type = temp.getType();\n" +
                    "if (type == 1) {\n" +
                    "    result = authResult;\n" +
                    "}\n" +
                    "if (type == 2) {\n" +
                    "    result = doorResult;\n" +
                    "}\n" +
                    "if (type == 3) {\n" +
                    "    result = statusResult;\n" +
                    "}\n" +
                    "return result;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // 验证有多个if节点
            long ifCount = response.getFlow().getNodes().stream()
                    .filter(n -> "if".equals(n.getType()))
                    .count();
            assertTrue(ifCount >= 3, "应该有至少3个if节点");
        }

        @Test
        @DisplayName("蓝牙规则 - 基本认证流程")
        void parseToVisual_bluetoothAuthFlow_shouldParse() {
            String script = "gmsConnected = component.getGlobalValue(GMS_CONNECTED);\n" +
                    "if (gmsConnected) {\n" +
                    "    reportData = buildReportData();\n" +
                    "    result = sendReport(reportData);\n" +
                    "}\n" +
                    "return result;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "if"));
        }
    }

    @Nested
    @DisplayName("门锁控制规则测试")
    class DoorControlRuleTest {

        @Test
        @DisplayName("门锁控制逻辑简化版应能解析")
        void parseToVisual_simplifiedDoorControl_shouldParse() {
            String script = "doorNo = bleDoorLockVO.getDoorNo();\n" +
                    "doorService = null;\n" +
                    "for (i = 0; i < doorServiceList.size(); i = i + 1) {\n" +
                    "    deviceService = doorServiceList.get(i);\n" +
                    "    dNo = deviceService.onReadOne(\"doorNo\");\n" +
                    "    if (dNo.equals(doorNo)) {\n" +
                    "        doorService = deviceService;\n" +
                    "    }\n" +
                    "}\n" +
                    "if (bleCommandTypeEnum == LOCK) {\n" +
                    "    resp = doorService.onCommand(closeFunction);\n" +
                    "    flag = resp.isSuccess();\n" +
                    "}\n" +
                    "return flag;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "for"));
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "if"));
        }

        @Test
        @DisplayName("门锁状态查询逻辑应能解析")
        void parseToVisual_doorStatusQuery_shouldParse() {
            String script = "doorStatus = doorService.onReadOne(\"doorStatus\");\n" +
                    "if (doorStatus.equals(\"OPEN\")) {\n" +
                    "    statusCode = 1;\n" +
                    "}\n" +
                    "if (doorStatus.equals(\"CLOSE\")) {\n" +
                    "    statusCode = 0;\n" +
                    "}\n" +
                    "response.setStatus(statusCode);\n" +
                    "return response;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("设备遍历规则测试")
    class DeviceIterationRuleTest {

        @Test
        @DisplayName("设备遍历逻辑应能解析")
        void parseToVisual_deviceIteration_shouldParse() {
            String script = "jsonArray = new JSONArray();\n" +
                    "for (i = 0; i < doorServiceList.size(); i = i + 1) {\n" +
                    "    deviceService = doorServiceList.get(i);\n" +
                    "    doorModel = new AppBluetoothDoorStatusModel();\n" +
                    "    deviceId = deviceService.getIotDevice().getDeviceId();\n" +
                    "    doorNo = deviceService.onReadOne(\"doorNo\").toString();\n" +
                    "    doorStatus = checkStatusResp.getParas();\n" +
                    "    if (doorStatus.equals(OPEN)) {\n" +
                    "        status = \"1\";\n" +
                    "    }\n" +
                    "    if (doorStatus.equals(CLOSE)) {\n" +
                    "        status = \"0\";\n" +
                    "    }\n" +
                    "    jsonArray.add(doorModel);\n" +
                    "}\n" +
                    "return jsonArray;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("批量设备处理应能解析")
        void parseToVisual_batchDeviceProcess_shouldParse() {
            String script = "result = new ArrayList();\n" +
                    "for (i = 0; i < devices.size(); i = i + 1) {\n" +
                    "    device = devices.get(i);\n" +
                    "    status = device.getStatus();\n" +
                    "    if (status == ONLINE) {\n" +
                    "        result.add(device);\n" +
                    "    }\n" +
                    "}\n" +
                    "return result;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "for"));
        }
    }

    @Nested
    @DisplayName("喇叭播报规则测试")
    class HornAnnouncementRuleTest {

        @Test
        @DisplayName("喇叭播报逻辑简化版应能解析")
        void parseToVisual_simplifiedHornAnnouncement_shouldParse() {
            // 简化版 - 循环外的条件判断
            String script = "innerHornService = getInnerHornService();\n" +
                    "outHornService = getOuterHornService();\n" +
                    "if (innerHornService != null) {\n" +
                    "    innerHornService.onCommand(playInnerAudio);\n" +
                    "}\n" +
                    "if (outHornService != null) {\n" +
                    "    outHornService.onCommand(playOuterAudio);\n" +
                    "}\n" +
                    "return success;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            // 验证有if节点
            long ifCount = response.getFlow().getNodes().stream()
                    .filter(n -> "if".equals(n.getType()))
                    .count();
            assertTrue(ifCount >= 2, "应该有至少2个if节点");
        }

        @Test
        @DisplayName("播报服务选择逻辑应能解析")
        void parseToVisual_hornServiceSelection_shouldParse() {
            String script = "selectedHorn = null;\n" +
                    "if (hornType == INDOOR) {\n" +
                    "    selectedHorn = innerHornService;\n" +
                    "}\n" +
                    "if (hornType == OUTDOOR) {\n" +
                    "    selectedHorn = outerHornService;\n" +
                    "}\n" +
                    "if (selectedHorn != null) {\n" +
                    "    selectedHorn.play(audioContent);\n" +
                    "}\n" +
                    "return true;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("错误处理规则测试")
    class ErrorHandlingRuleTest {

        @Test
        @DisplayName("条件分支和错误处理应能解析")
        void parseToVisual_conditionalWithError_shouldParse() {
            String script = "gmsConnected = component.getGlobalValue(GMS_CONNECTED);\n" +
                    "if (gmsConnected) {\n" +
                    "    reportData = buildReportData();\n" +
                    "    AckAdapter.eventReport(type, gmsModel, localDoorAckModel);\n" +
                    "}\n" +
                    "if (doorOneOpen) {\n" +
                    "    flag = true;\n" +
                    "}\n" +
                    "if (flag == false) {\n" +
                    "    appBluetoothCommandReponse.setCode(100002);\n" +
                    "    innerHornService.onCommand(playFailAudio);\n" +
                    "}\n" +
                    "doorModel.setStatus(flag);\n" +
                    "result.add(doorModel);\n" +
                    "return result;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("多重错误检查逻辑应能解析")
        void parseToVisual_multiErrorCheck_shouldParse() {
            String script = "errorCode = 0;\n" +
                    "if (param1 == null) {\n" +
                    "    errorCode = 1001;\n" +
                    "}\n" +
                    "if (param2 == null) {\n" +
                    "    errorCode = 1002;\n" +
                    "}\n" +
                    "if (errorCode != 0) {\n" +
                    "    response.setErrorCode(errorCode);\n" +
                    "    return response;\n" +
                    "}\n" +
                    "result = processData(param1, param2);\n" +
                    "return result;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    @Nested
    @DisplayName("复杂嵌套结构测试")
    class ComplexNestingTest {

        @Test
        @DisplayName("循环内多重条件应能解析")
        void parseToVisual_loopWithMultipleConditions_shouldParse() {
            String script = "totalCount = 0;\n" +
                    "for (i = 0; i < list.size(); i = i + 1) {\n" +
                    "    item = list.get(i);\n" +
                    "    if (item.isValid()) {\n" +
                    "        if (item.getType() == TYPE_A) {\n" +
                    "            totalCount = totalCount + 1;\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n" +
                    "return totalCount;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("多层循环嵌套应能解析")
        void parseToVisual_nestedLoops_shouldParse() {
            String script = "matrix = new ArrayList();\n" +
                    "for (i = 0; i < rows; i = i + 1) {\n" +
                    "    row = new ArrayList();\n" +
                    "    for (j = 0; j < cols; j = j + 1) {\n" +
                    "        value = calculateValue(i, j);\n" +
                    "        row.add(value);\n" +
                    "    }\n" +
                    "    matrix.add(row);\n" +
                    "}\n" +
                    "return matrix;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("while循环内条件判断应能解析")
        void parseToVisual_whileWithConditions_shouldParse() {
            String script = "retry = 0;\n" +
                    "success = false;\n" +
                    "while (retry < 3) {\n" +
                    "    result = tryConnect();\n" +
                    "    if (result.isSuccess()) {\n" +
                    "        success = true;\n" +
                    "    }\n" +
                    "    retry = retry + 1;\n" +
                    "}\n" +
                    "return success;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
            assertTrue(hasNodeOfType(response.getFlow().getNodes(), "while"));
        }
    }

    @Nested
    @DisplayName("实际业务场景模拟测试")
    class RealBusinessScenarioTest {

        @Test
        @DisplayName("订单处理流程应能解析")
        void parseToVisual_orderProcessFlow_shouldParse() {
            String script = "order = getOrder(orderId);\n" +
                    "if (order == null) {\n" +
                    "    return errorResponse(\"Order not found\");\n" +
                    "}\n" +
                    "status = order.getStatus();\n" +
                    "if (status == PENDING) {\n" +
                    "    order.setStatus(PROCESSING);\n" +
                    "    saveOrder(order);\n" +
                    "}\n" +
                    "if (status == PROCESSING) {\n" +
                    "    items = order.getItems();\n" +
                    "    for (i = 0; i < items.size(); i = i + 1) {\n" +
                    "        item = items.get(i);\n" +
                    "        processItem(item);\n" +
                    "    }\n" +
                    "    order.setStatus(COMPLETED);\n" +
                    "}\n" +
                    "return order;";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("用户认证流程应能解析")
        void parseToVisual_userAuthFlow_shouldParse() {
            String script = "user = findUser(username);\n" +
                    "if (user == null) {\n" +
                    "    return authFailed(\"User not found\");\n" +
                    "}\n" +
                    "if (user.isLocked()) {\n" +
                    "    return authFailed(\"Account locked\");\n" +
                    "}\n" +
                    "passwordMatch = checkPassword(user, password);\n" +
                    "if (passwordMatch == false) {\n" +
                    "    incrementFailCount(user);\n" +
                    "    return authFailed(\"Invalid password\");\n" +
                    "}\n" +
                    "token = generateToken(user);\n" +
                    "return authSuccess(token);";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }

        @Test
        @DisplayName("数据同步流程应能解析")
        void parseToVisual_dataSyncFlow_shouldParse() {
            String script = "remoteData = fetchRemoteData();\n" +
                    "localData = getLocalData();\n" +
                    "changes = new ArrayList();\n" +
                    "for (i = 0; i < remoteData.size(); i = i + 1) {\n" +
                    "    remoteItem = remoteData.get(i);\n" +
                    "    localItem = findLocal(remoteItem.getId());\n" +
                    "    if (localItem == null) {\n" +
                    "        changes.add(createChange(\"ADD\", remoteItem));\n" +
                    "    }\n" +
                    "    if (localItem != null) {\n" +
                    "        if (remoteItem.getVersion() > localItem.getVersion()) {\n" +
                    "            changes.add(createChange(\"UPDATE\", remoteItem));\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n" +
                    "applyChanges(changes);\n" +
                    "return changes.size();";

            ParseToVisualRequest request = ParseToVisualRequest.builder()
                    .script(script)
                    .build();

            ParseToVisualResponse response = parseService.parseToVisual(request);

            assertTrue(response.isSuccess());
            assertNotNull(response.getFlow());
        }
    }

    // ==================== 辅助方法 ====================

    private boolean hasNodeOfType(List<VisualNode> nodes, String type) {
        if (nodes == null) return false;
        return nodes.stream().anyMatch(n -> type.equals(n.getType()));
    }
}

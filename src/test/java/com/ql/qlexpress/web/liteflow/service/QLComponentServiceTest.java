package com.ql.qlexpress.web.liteflow.service;

import com.ql.qlexpress.web.exception.BusinessException;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import com.ql.qlexpress.web.liteflow.repository.QLComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QLComponent 服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QLComponent 服务测试")
class QLComponentServiceTest {

    @Mock
    private QLComponentRepository componentRepository;

    @Mock
    private LiteFlowDynamicService liteFlowDynamicService;

    @InjectMocks
    private QLComponentService componentService;

    private QLComponent testScriptComponent;
    private QLComponent testBooleanComponent;
    private QLComponent testSwitchComponent;

    @BeforeEach
    void setUp() {
        // 创建测试用的脚本组件
        testScriptComponent = QLComponent.builder()
                .componentId("testScript")
                .componentName("测试脚本组件")
                .description("一个测试用的脚本组件")
                .category("基础")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("return 'success';")
                .language("qlexpress")
                .enabled(true)
                .build();

        // 创建测试用的布尔组件
        testBooleanComponent = QLComponent.builder()
                .componentId("testBoolean")
                .componentName("测试布尔组件")
                .category("状态检查")
                .componentType(QLComponent.ComponentType.BOOLEAN)
                .script("return true;")
                .language("qlexpress")
                .enabled(true)
                .build();

        // 创建测试用的路由组件 - 使用三元表达式 (QLExpress4 语法)
        testSwitchComponent = QLComponent.builder()
                .componentId("testSwitch")
                .componentName("测试路由组件")
                .category("路由")
                .componentType(QLComponent.ComponentType.SWITCH)
                .script("type == 1 ? 'A' : (type == 2 ? 'B' : 'DEFAULT')")
                .language("qlexpress")
                .switchBranches(Arrays.asList(
                        QLComponent.SwitchBranch.builder().branchId("A").branchName("分支A").build(),
                        QLComponent.SwitchBranch.builder().branchId("B").branchName("分支B").build(),
                        QLComponent.SwitchBranch.builder().branchId("DEFAULT").branchName("默认").build()
                ))
                .defaultBranch("DEFAULT")
                .enabled(true)
                .build();
    }

    // ==================== 创建组件测试 ====================

    @Test
    @DisplayName("创建组件成功")
    void testCreateComponentSuccess() {
        when(componentRepository.existsById("testScript")).thenReturn(false);
        when(componentRepository.save(any(QLComponent.class))).thenReturn(testScriptComponent);
        doNothing().when(liteFlowDynamicService).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());

        QLComponent result = componentService.createComponent(testScriptComponent);

        assertNotNull(result);
        assertEquals("testScript", result.getComponentId());
        verify(componentRepository).save(any(QLComponent.class));
        verify(liteFlowDynamicService).addScriptNode(
                eq("testScript"),
                eq("测试脚本组件"),
                anyString(),
                eq("script"),
                eq("qlexpress")
        );
    }

    @Test
    @DisplayName("创建组件失败 - ID已存在")
    void testCreateComponentFailIdExists() {
        when(componentRepository.existsById("testScript")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            componentService.createComponent(testScriptComponent);
        });

        assertTrue(exception.getMessage().contains("已存在"));
        verify(componentRepository, never()).save(any(QLComponent.class));
    }

    @Test
    @DisplayName("创建组件失败 - 脚本语法错误")
    void testCreateComponentFailInvalidScript() {
        QLComponent invalidComponent = QLComponent.builder()
                .componentId("invalid")
                .componentName("无效组件")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("this is not valid script {{{{")
                .language("qlexpress")
                .enabled(true)
                .build();

        when(componentRepository.existsById("invalid")).thenReturn(false);

        // 脚本语法错误应该抛出异常
        assertThrows(Exception.class, () -> {
            componentService.createComponent(invalidComponent);
        });
    }

    // ==================== 更新组件测试 ====================

    @Test
    @DisplayName("更新组件成功")
    void testUpdateComponentSuccess() {
        when(componentRepository.findById("testScript")).thenReturn(Optional.of(testScriptComponent));
        when(componentRepository.save(any(QLComponent.class))).thenReturn(testScriptComponent);
        doNothing().when(liteFlowDynamicService).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());

        testScriptComponent.setComponentName("更新后的名称");
        QLComponent result = componentService.updateComponent("testScript", testScriptComponent);

        assertNotNull(result);
        verify(componentRepository).save(any(QLComponent.class));
    }

    @Test
    @DisplayName("更新组件失败 - 组件不存在")
    void testUpdateComponentFailNotFound() {
        when(componentRepository.findById("notExist")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            componentService.updateComponent("notExist", testScriptComponent);
        });

        assertTrue(exception.getMessage().contains("不存在"));
    }

    // ==================== 查询组件测试 ====================

    @Test
    @DisplayName("获取组件详情")
    void testGetComponent() {
        when(componentRepository.findById("testScript")).thenReturn(Optional.of(testScriptComponent));

        QLComponent result = componentService.getComponent("testScript");

        assertNotNull(result);
        assertEquals("testScript", result.getComponentId());
    }

    @Test
    @DisplayName("获取组件失败 - 不存在")
    void testGetComponentNotFound() {
        when(componentRepository.findById("notExist")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> {
            componentService.getComponent("notExist");
        });
    }

    @Test
    @DisplayName("获取组件列表 - 无过滤")
    void testListComponentsNoFilter() {
        when(componentRepository.findAll()).thenReturn(Arrays.asList(
                testScriptComponent, testBooleanComponent, testSwitchComponent));

        List<QLComponent> result = componentService.listComponents(null, null, null);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("获取组件列表 - 按分类过滤")
    void testListComponentsByCategory() {
        when(componentRepository.findByCategory("基础")).thenReturn(Collections.singletonList(testScriptComponent));

        List<QLComponent> result = componentService.listComponents("基础", null, null);

        assertEquals(1, result.size());
        assertEquals("基础", result.get(0).getCategory());
    }

    @Test
    @DisplayName("获取组件列表 - 按类型过滤")
    void testListComponentsByType() {
        when(componentRepository.findAll()).thenReturn(Arrays.asList(
                testScriptComponent, testBooleanComponent, testSwitchComponent));

        List<QLComponent> result = componentService.listComponents(null, QLComponent.ComponentType.BOOLEAN, null);

        assertEquals(1, result.size());
        assertEquals(QLComponent.ComponentType.BOOLEAN, result.get(0).getComponentType());
    }

    // ==================== 删除组件测试 ====================

    @Test
    @DisplayName("删除组件成功")
    void testDeleteComponentSuccess() {
        when(componentRepository.existsById("testScript")).thenReturn(true);
        doNothing().when(componentRepository).deleteById("testScript");

        componentService.deleteComponent("testScript");

        verify(componentRepository).deleteById("testScript");
    }

    @Test
    @DisplayName("删除组件失败 - 不存在")
    void testDeleteComponentNotFound() {
        when(componentRepository.existsById("notExist")).thenReturn(false);

        assertThrows(BusinessException.class, () -> {
            componentService.deleteComponent("notExist");
        });
    }

    // ==================== 脚本验证测试 ====================

    @Test
    @DisplayName("验证有效的脚本")
    void testValidateValidScript() {
        QLComponentService.ScriptValidationResult result =
                componentService.validateScript(testScriptComponent);

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("验证空脚本")
    void testValidateEmptyScript() {
        QLComponent emptyScript = QLComponent.builder()
                .componentId("empty")
                .componentName("空脚本")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("")
                .build();

        QLComponentService.ScriptValidationResult result =
                componentService.validateScript(emptyScript);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("不能为空")));
    }

    @Test
    @DisplayName("验证BOOLEAN组件无return语句给出警告")
    void testValidateBooleanWithoutReturn() {
        QLComponent booleanNoReturn = QLComponent.builder()
                .componentId("boolNoReturn")
                .componentName("无返回的布尔组件")
                .componentType(QLComponent.ComponentType.BOOLEAN)
                .script("x = 1;")
                .language("qlexpress")
                .build();

        QLComponentService.ScriptValidationResult result =
                componentService.validateScript(booleanNoReturn);

        assertTrue(result.isValid()); // 语法正确
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("return")));
    }

    @Test
    @DisplayName("验证SWITCH组件无分支定义给出警告")
    void testValidateSwitchWithoutBranches() {
        QLComponent switchNoBranch = QLComponent.builder()
                .componentId("switchNoBranch")
                .componentName("无分支的路由组件")
                .componentType(QLComponent.ComponentType.SWITCH)
                .script("return 'A';")
                .language("qlexpress")
                .build();

        QLComponentService.ScriptValidationResult result =
                componentService.validateScript(switchNoBranch);

        assertTrue(result.isValid()); // 语法正确
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("switchBranches")));
    }

    // ==================== 组件测试执行 ====================

    @Test
    @DisplayName("测试SCRIPT组件执行")
    void testComponentExecutionScript() {
        when(componentRepository.findById("testScript")).thenReturn(Optional.of(testScriptComponent));

        QLComponentService.ComponentTestResult result =
                componentService.testComponent("testScript", null);

        assertTrue(result.isSuccess());
        assertNotNull(result.getContext());
    }

    @Test
    @DisplayName("测试BOOLEAN组件执行")
    void testComponentExecutionBoolean() {
        when(componentRepository.findById("testBoolean")).thenReturn(Optional.of(testBooleanComponent));

        QLComponentService.ComponentTestResult result =
                componentService.testComponent("testBoolean", null);

        assertTrue(result.isSuccess());
        assertEquals(true, result.getReturnValue());
    }

    @Test
    @DisplayName("测试SWITCH组件执行")
    void testComponentExecutionSwitch() {
        when(componentRepository.findById("testSwitch")).thenReturn(Optional.of(testSwitchComponent));

        // 测试默认分支 - 传入 type=0
        Map<String, Object> defaultParams = new HashMap<>();
        defaultParams.put("type", 0);
        QLComponentService.ComponentTestResult result1 =
                componentService.testComponent("testSwitch", defaultParams);
        assertTrue(result1.isSuccess());
        assertEquals("DEFAULT", result1.getReturnValue());

        // 测试分支A
        Map<String, Object> paramsA = new HashMap<>();
        paramsA.put("type", 1);
        QLComponentService.ComponentTestResult result2 =
                componentService.testComponent("testSwitch", paramsA);
        assertTrue(result2.isSuccess());
        assertEquals("A", result2.getReturnValue());

        // 测试分支B
        Map<String, Object> paramsB = new HashMap<>();
        paramsB.put("type", 2);
        QLComponentService.ComponentTestResult result3 =
                componentService.testComponent("testSwitch", paramsB);
        assertTrue(result3.isSuccess());
        assertEquals("B", result3.getReturnValue());
    }

    @Test
    @DisplayName("测试组件执行 - 带输入参数")
    void testComponentExecutionWithParams() {
        QLComponent paramComponent = QLComponent.builder()
                .componentId("paramTest")
                .componentName("参数测试组件")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("return num1 + num2;")
                .language("qlexpress")
                .build();

        Map<String, Object> params = new HashMap<>();
        params.put("num1", 10);
        params.put("num2", 20);

        QLComponentService.ComponentTestResult result =
                componentService.testComponentExecution(paramComponent, params);

        assertTrue(result.isSuccess());
        assertEquals(30, result.getReturnValue());
    }

    // ==================== 分类管理测试 ====================

    @Test
    @DisplayName("获取所有分类")
    void testGetAllCategories() {
        when(componentRepository.getAllCategories())
                .thenReturn(new HashSet<>(Arrays.asList("基础", "状态检查", "路由")));

        Set<String> categories = componentService.getAllCategories();

        assertEquals(3, categories.size());
        assertTrue(categories.contains("基础"));
    }

    @Test
    @DisplayName("获取分类统计")
    void testGetCategoryStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("基础", 5);
        stats.put("状态检查", 8);
        when(componentRepository.countByCategory()).thenReturn(stats);

        Map<String, Integer> result = componentService.getCategoryStats();

        assertEquals(5, result.get("基础").intValue());
        assertEquals(8, result.get("状态检查").intValue());
    }

    // ==================== LiteFlow 同步测试 ====================

    @Test
    @DisplayName("同步组件到LiteFlow")
    void testSyncToLiteFlow() {
        doNothing().when(liteFlowDynamicService).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());

        componentService.syncToLiteFlow(testScriptComponent);

        verify(liteFlowDynamicService).addScriptNode(
                "testScript",
                "测试脚本组件",
                testScriptComponent.getScript(),
                "script",
                "qlexpress"
        );
    }

    @Test
    @DisplayName("禁用的组件不同步到LiteFlow")
    void testSyncDisabledComponent() {
        testScriptComponent.setEnabled(false);

        componentService.syncToLiteFlow(testScriptComponent);

        verify(liteFlowDynamicService, never()).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("同步所有组件到LiteFlow")
    void testSyncAllToLiteFlow() {
        when(componentRepository.findByEnabled(true))
                .thenReturn(Arrays.asList(testScriptComponent, testBooleanComponent));
        doNothing().when(liteFlowDynamicService).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());

        int count = componentService.syncAllToLiteFlow();

        assertEquals(2, count);
        verify(liteFlowDynamicService, times(2)).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ==================== 批量创建测试 ====================

    @Test
    @DisplayName("批量创建组件")
    void testBatchCreateComponents() {
        when(componentRepository.existsById(anyString())).thenReturn(false);
        when(componentRepository.save(any(QLComponent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(liteFlowDynamicService).addScriptNode(anyString(), anyString(), anyString(), anyString(), anyString());

        List<QLComponent> components = Arrays.asList(testScriptComponent, testBooleanComponent);
        List<QLComponent> result = componentService.batchCreateComponents(components);

        assertEquals(2, result.size());
    }
}

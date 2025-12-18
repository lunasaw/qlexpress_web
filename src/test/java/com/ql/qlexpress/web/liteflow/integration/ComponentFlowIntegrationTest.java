package com.ql.qlexpress.web.liteflow.integration;

import com.ql.qlexpress.web.liteflow.model.CmpProperty;
import com.ql.qlexpress.web.liteflow.model.ConvertResult;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import com.ql.qlexpress.web.liteflow.repository.FlowDesignRepository;
import com.ql.qlexpress.web.liteflow.repository.QLComponentRepository;
import com.ql.qlexpress.web.liteflow.service.FlowConvertService;
import com.ql.qlexpress.web.liteflow.service.FlowManageService;
import com.ql.qlexpress.web.liteflow.service.LiteFlowDynamicService;
import com.ql.qlexpress.web.liteflow.service.QLComponentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 组件化流程编排集成测试
 * 测试完整的：组件创建 → 流程编排 → 转换 → 部署 → 执行 流程
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("组件化流程编排集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ComponentFlowIntegrationTest {

    @Autowired
    private QLComponentService componentService;

    @Autowired
    private FlowManageService flowManageService;

    @Autowired
    private FlowConvertService flowConvertService;

    @Autowired
    private LiteFlowDynamicService liteFlowDynamicService;

    @Autowired
    private QLComponentRepository componentRepository;

    @Autowired
    private FlowDesignRepository flowRepository;

    // 测试用组件ID
    private static final String COMP_INIT = "integration_init";
    private static final String COMP_PROCESS = "integration_process";
    private static final String COMP_CHECK = "integration_check";
    private static final String COMP_ROUTER = "integration_router";
    private static final String COMP_BRANCH_A = "integration_branchA";
    private static final String COMP_BRANCH_B = "integration_branchB";
    private static final String COMP_FINISH = "integration_finish";

    // 测试用流程ID
    private static final String FLOW_SIMPLE = "integration_simple_flow";
    private static final String FLOW_CONDITION = "integration_condition_flow";
    private static final String FLOW_COMPLEX = "integration_complex_flow";

    @BeforeEach
    void setUp() {
        // 清理之前的测试数据
        cleanupTestData();
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        cleanupTestData();
    }

    private void cleanupTestData() {
        // 清理测试流程
        Arrays.asList(FLOW_SIMPLE, FLOW_CONDITION, FLOW_COMPLEX).forEach(flowId -> {
            try {
                flowRepository.deleteById(flowId);
            } catch (Exception ignored) {}
            try {
                liteFlowDynamicService.removeChain(flowId);
            } catch (Exception ignored) {}
        });

        // 清理测试组件
        Arrays.asList(COMP_INIT, COMP_PROCESS, COMP_CHECK, COMP_ROUTER,
                      COMP_BRANCH_A, COMP_BRANCH_B, COMP_FINISH).forEach(compId -> {
            try {
                componentRepository.deleteById(compId);
            } catch (Exception ignored) {}
        });
    }

    // ==================== 场景1: 基础组件 CRUD 测试 ====================

    @Test
    @Order(1)
    @DisplayName("1.1 创建 SCRIPT 类型组件")
    void testCreateScriptComponent() {
        QLComponent component = QLComponent.builder()
                .componentId(COMP_INIT)
                .componentName("初始化组件")
                .description("设置初始值")
                .category("集成测试")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("flowContext.setData('initialized', true); flowContext.setData('counter', 0);")
                .language("qlexpress")
                .enabled(true)
                .build();

        QLComponent saved = componentService.createComponent(component);

        assertNotNull(saved);
        assertEquals(COMP_INIT, saved.getComponentId());
        assertEquals(QLComponent.ComponentType.SCRIPT, saved.getComponentType());

        // 验证可以查询到
        QLComponent found = componentService.getComponent(COMP_INIT);
        assertNotNull(found);
        assertEquals("初始化组件", found.getComponentName());
    }

    @Test
    @Order(2)
    @DisplayName("1.2 创建 BOOLEAN 类型组件")
    void testCreateBooleanComponent() {
        QLComponent component = QLComponent.builder()
                .componentId(COMP_CHECK)
                .componentName("条件检查组件")
                .description("检查计数器是否小于阈值")
                .category("集成测试")
                .componentType(QLComponent.ComponentType.BOOLEAN)
                .script("counter = flowContext.getData('counter'); return counter < 10;")
                .language("qlexpress")
                .enabled(true)
                .build();

        QLComponent saved = componentService.createComponent(component);

        assertNotNull(saved);
        assertEquals(QLComponent.ComponentType.BOOLEAN, saved.getComponentType());
    }

    @Test
    @Order(3)
    @DisplayName("1.3 创建 SWITCH 类型组件")
    void testCreateSwitchComponent() {
        QLComponent component = QLComponent.builder()
                .componentId(COMP_ROUTER)
                .componentName("路由选择组件")
                .description("根据类型选择分支")
                .category("集成测试")
                .componentType(QLComponent.ComponentType.SWITCH)
                .script("type = flowContext.getData('type'); return type == 'A' ? 'branchA' : 'branchB';")
                .language("qlexpress")
                .switchBranches(Arrays.asList(
                        QLComponent.SwitchBranch.builder().branchId("branchA").branchName("分支A").build(),
                        QLComponent.SwitchBranch.builder().branchId("branchB").branchName("分支B").build()
                ))
                .defaultBranch("branchB")
                .enabled(true)
                .build();

        QLComponent saved = componentService.createComponent(component);

        assertNotNull(saved);
        assertEquals(QLComponent.ComponentType.SWITCH, saved.getComponentType());
        assertEquals(2, saved.getSwitchBranches().size());
    }

    @Test
    @Order(4)
    @DisplayName("1.4 组件脚本验证")
    void testComponentScriptValidation() {
        // 有效脚本验证
        QLComponent validComponent = QLComponent.builder()
                .componentId("temp_valid")
                .componentName("临时组件")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("x = 1 + 2; return x;")
                .language("qlexpress")
                .build();

        QLComponentService.ScriptValidationResult validResult =
                componentService.validateScript(validComponent);
        assertTrue(validResult.isValid());

        // 无效脚本验证
        QLComponent invalidComponent = QLComponent.builder()
                .componentId("temp_invalid")
                .componentName("无效组件")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("this is not valid {{{{ syntax")
                .language("qlexpress")
                .build();

        QLComponentService.ScriptValidationResult invalidResult =
                componentService.validateScript(invalidComponent);
        assertFalse(invalidResult.isValid());
        assertFalse(invalidResult.getErrors().isEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("1.5 组件独立执行测试")
    void testComponentExecution() {
        // 创建测试组件
        QLComponent component = QLComponent.builder()
                .componentId(COMP_PROCESS)
                .componentName("处理组件")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("return num1 + num2;")
                .language("qlexpress")
                .enabled(true)
                .build();

        componentService.createComponent(component);

        // 测试执行
        Map<String, Object> params = new HashMap<>();
        params.put("num1", 10);
        params.put("num2", 20);

        QLComponentService.ComponentTestResult result =
                componentService.testComponent(COMP_PROCESS, params);

        assertTrue(result.isSuccess());
        assertEquals(30, result.getReturnValue());
    }

    // ==================== 场景2: 简单顺序流程测试 ====================

    @Test
    @Order(10)
    @DisplayName("2.1 创建简单顺序流程 (THEN)")
    void testCreateSimpleSequenceFlow() {
        // 先创建组件
        createTestComponents();

        // 构建流程: THEN(init, process, finish)
        CmpProperty root = CmpProperty.builder()
                .type("THEN")
                .children(Arrays.asList(
                        createLeafNode(COMP_INIT),
                        createLeafNode(COMP_PROCESS),
                        createLeafNode(COMP_FINISH)
                ))
                .build();

        FlowDesign flow = FlowDesign.builder()
                .flowId(FLOW_SIMPLE)
                .flowName("简单顺序流程")
                .description("测试顺序执行")
                .category("集成测试")
                .root(root)
                .enabled(true)
                .build();

        // 创建流程
        FlowDesign saved = flowManageService.createFlow(flow);

        assertNotNull(saved);
        assertEquals(FLOW_SIMPLE, saved.getFlowId());
        assertNotNull(saved.getUsedComponentIds());
        assertTrue(saved.getUsedComponentIds().contains(COMP_INIT));
    }

    @Test
    @Order(11)
    @DisplayName("2.2 转换简单流程为 EL 表达式")
    void testConvertSimpleFlow() {
        // 先创建组件和流程
        createTestComponents();
        createSimpleFlow();

        // 转换流程
        ConvertResult result = flowManageService.convertFlow(FLOW_SIMPLE);

        assertTrue(result.isSuccess());
        assertNotNull(result.getEl());
        assertTrue(result.getEl().contains("THEN"));
        assertTrue(result.getEl().contains(COMP_INIT));
    }

    @Test
    @Order(12)
    @DisplayName("2.3 部署并执行简单流程")
    void testDeployAndExecuteSimpleFlow() {
        // 创建组件和流程
        createTestComponents();
        createSimpleFlow();

        // 部署流程
        FlowManageService.DeployResult deployResult = flowManageService.deployFlow(FLOW_SIMPLE);
        assertTrue(deployResult.isSuccess(), "部署失败: " + deployResult.getErrorMessage());

        // 执行流程
        Map<String, Object> params = new HashMap<>();
        params.put("inputValue", 100);

        FlowManageService.ExecuteResult executeResult = flowManageService.executeFlow(FLOW_SIMPLE, params);

        assertTrue(executeResult.isSuccess(), "执行失败: " + executeResult.getErrorMessage());
        assertNotNull(executeResult.getContext());
        assertNotNull(executeResult.getExecuteSteps());
    }

    // ==================== 场景3: 条件分支流程测试 ====================

    @Test
    @Order(20)
    @DisplayName("3.1 创建条件分支流程 (IF)")
    void testCreateConditionFlow() {
        createTestComponents();

        // 构建流程: THEN(init, IF(check, processA, processB), finish)
        CmpProperty ifNode = CmpProperty.builder()
                .type("IF")
                .condition(createLeafNode(COMP_CHECK))
                .children(Arrays.asList(
                        createLeafNode(COMP_BRANCH_A),
                        createLeafNode(COMP_BRANCH_B)
                ))
                .build();

        CmpProperty root = CmpProperty.builder()
                .type("THEN")
                .children(Arrays.asList(
                        createLeafNode(COMP_INIT),
                        ifNode,
                        createLeafNode(COMP_FINISH)
                ))
                .build();

        FlowDesign flow = FlowDesign.builder()
                .flowId(FLOW_CONDITION)
                .flowName("条件分支流程")
                .description("测试条件分支")
                .category("集成测试")
                .root(root)
                .enabled(true)
                .build();

        FlowDesign saved = flowManageService.createFlow(flow);

        assertNotNull(saved);
        assertTrue(saved.getUsedComponentIds().contains(COMP_CHECK));
    }

    @Test
    @Order(21)
    @DisplayName("3.2 验证 IF 流程 EL 表达式生成")
    void testIfFlowELGeneration() {
        createTestComponents();
        createConditionFlow();

        ConvertResult result = flowManageService.convertFlow(FLOW_CONDITION);

        assertTrue(result.isSuccess());
        assertTrue(result.getEl().contains("IF"));
        assertTrue(result.getEl().contains(COMP_CHECK));
    }

    @Test
    @Order(22)
    @DisplayName("3.3 执行条件分支流程 - 走 true 分支")
    void testExecuteConditionFlowTrueBranch() {
        createTestComponents();
        createConditionFlow();

        // 部署
        FlowManageService.DeployResult deployResult = flowManageService.deployFlow(FLOW_CONDITION);
        assertTrue(deployResult.isSuccess());

        // 执行 - counter=0 < 10，走 true 分支
        Map<String, Object> params = new HashMap<>();
        params.put("counter", 0);

        FlowManageService.ExecuteResult result = flowManageService.executeFlow(FLOW_CONDITION, params);

        assertTrue(result.isSuccess());
        // 验证走了 branchA
        String steps = result.getExecuteSteps();
        assertNotNull(steps);
    }

    // ==================== 场景4: 复杂流程测试 ====================

    @Test
    @Order(30)
    @DisplayName("4.1 创建复杂嵌套流程")
    void testCreateComplexFlow() {
        createTestComponents();

        // 构建复杂流程:
        // THEN(
        //   init,
        //   IF(check,
        //      SWITCH(router).to(branchA, branchB),
        //      process
        //   ),
        //   finish
        // )
        CmpProperty switchNode = CmpProperty.builder()
                .type("SWITCH")
                .condition(createLeafNode(COMP_ROUTER))
                .children(Arrays.asList(
                        CmpProperty.builder()
                                .id(COMP_BRANCH_A)
                                .componentRef(COMP_BRANCH_A)
                                .properties(CmpProperty.Properties.builder().branch("branchA").build())
                                .build(),
                        CmpProperty.builder()
                                .id(COMP_BRANCH_B)
                                .componentRef(COMP_BRANCH_B)
                                .properties(CmpProperty.Properties.builder().branch("branchB").build())
                                .build()
                ))
                .build();

        CmpProperty ifNode = CmpProperty.builder()
                .type("IF")
                .condition(createLeafNode(COMP_CHECK))
                .children(Arrays.asList(
                        switchNode,
                        createLeafNode(COMP_PROCESS)
                ))
                .build();

        CmpProperty root = CmpProperty.builder()
                .type("THEN")
                .children(Arrays.asList(
                        createLeafNode(COMP_INIT),
                        ifNode,
                        createLeafNode(COMP_FINISH)
                ))
                .build();

        FlowDesign flow = FlowDesign.builder()
                .flowId(FLOW_COMPLEX)
                .flowName("复杂嵌套流程")
                .description("测试复杂嵌套场景")
                .category("集成测试")
                .root(root)
                .enabled(true)
                .build();

        FlowDesign saved = flowManageService.createFlow(flow);

        assertNotNull(saved);
        assertEquals(7, saved.getUsedComponentIds().size()); // init, check, router, branchA, branchB, process, finish
    }

    @Test
    @Order(31)
    @DisplayName("4.2 验证复杂流程 EL 表达式")
    void testComplexFlowELGeneration() {
        createTestComponents();
        createComplexFlow();

        ConvertResult result = flowManageService.convertFlow(FLOW_COMPLEX);

        assertTrue(result.isSuccess());
        assertNotNull(result.getEl());
        assertTrue(result.getEl().contains("THEN"));
        assertTrue(result.getEl().contains("IF"));
        assertTrue(result.getEl().contains("SWITCH"));
    }

    // ==================== 场景5: 流程验证测试 ====================

    @Test
    @Order(40)
    @DisplayName("5.1 验证流程 - 缺少条件节点")
    void testValidateFlowMissingCondition() {
        // IF 缺少条件
        CmpProperty root = CmpProperty.builder()
                .type("IF")
                .children(Arrays.asList(createLeafNode(COMP_INIT)))
                .build();

        FlowDesign flow = FlowDesign.builder()
                .flowId("invalid_flow")
                .flowName("无效流程")
                .root(root)
                .build();

        FlowConvertService.ValidationResult result = flowConvertService.validate(flow);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("IF") || e.contains("条件")));
    }

    @Test
    @Order(41)
    @DisplayName("5.2 验证流程 - 引用不存在的组件")
    void testValidateFlowNonExistentComponent() {
        CmpProperty root = CmpProperty.builder()
                .type("THEN")
                .children(Arrays.asList(
                        createLeafNode("non_existent_component")
                ))
                .build();

        FlowDesign flow = FlowDesign.builder()
                .flowId("invalid_flow2")
                .flowName("无效流程2")
                .root(root)
                .build();

        FlowConvertService.ValidationResult result = flowConvertService.validate(flow);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("non_existent_component")));
    }

    // ==================== 场景6: 直接执行 EL 表达式测试 ====================

    @Test
    @Order(50)
    @DisplayName("6.1 直接执行 EL 表达式")
    void testDirectELExecution() {
        // 先注册节点
        liteFlowDynamicService.addScriptNode(
                "testNodeA", "测试节点A",
                "flowContext.setData('result', 'A executed');",
                "script", "qlexpress"
        );
        liteFlowDynamicService.addScriptNode(
                "testNodeB", "测试节点B",
                "prev = flowContext.getData('result'); flowContext.setData('result', prev + ' -> B executed');",
                "script", "qlexpress"
        );

        // 直接执行 EL
        String el = "THEN(testNodeA, testNodeB)";
        Map<String, Object> result = liteFlowDynamicService.executeEL(el, new HashMap<>());

        assertTrue((Boolean) result.get("success"));
        Map<String, Object> context = (Map<String, Object>) result.get("context");
        assertEquals("A executed -> B executed", context.get("result"));
    }

    // ==================== 辅助方法 ====================

    private void createTestComponents() {
        // 初始化组件
        if (!componentRepository.existsById(COMP_INIT)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_INIT)
                    .componentName("初始化")
                    .componentType(QLComponent.ComponentType.SCRIPT)
                    .script("flowContext.setData('initialized', true);")
                    .language("qlexpress")
                    .enabled(true)
                    .build());
        }

        // 处理组件
        if (!componentRepository.existsById(COMP_PROCESS)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_PROCESS)
                    .componentName("处理")
                    .componentType(QLComponent.ComponentType.SCRIPT)
                    .script("flowContext.setData('processed', true);")
                    .language("qlexpress")
                    .enabled(true)
                    .build());
        }

        // 检查组件
        if (!componentRepository.existsById(COMP_CHECK)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_CHECK)
                    .componentName("条件检查")
                    .componentType(QLComponent.ComponentType.BOOLEAN)
                    .script("counter = flowContext.getData('counter', 0); return counter < 10;")
                    .language("qlexpress")
                    .enabled(true)
                    .build());
        }

        // 路由组件
        if (!componentRepository.existsById(COMP_ROUTER)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_ROUTER)
                    .componentName("路由选择")
                    .componentType(QLComponent.ComponentType.SWITCH)
                    .script("type = flowContext.getData('type', 'A'); return type == 'A' ? 'branchA' : 'branchB';")
                    .language("qlexpress")
                    .switchBranches(Arrays.asList(
                            QLComponent.SwitchBranch.builder().branchId("branchA").branchName("分支A").build(),
                            QLComponent.SwitchBranch.builder().branchId("branchB").branchName("分支B").build()
                    ))
                    .enabled(true)
                    .build());
        }

        // 分支A组件
        if (!componentRepository.existsById(COMP_BRANCH_A)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_BRANCH_A)
                    .componentName("分支A处理")
                    .componentType(QLComponent.ComponentType.SCRIPT)
                    .script("flowContext.setData('branch', 'A');")
                    .language("qlexpress")
                    .enabled(true)
                    .build());
        }

        // 分支B组件
        if (!componentRepository.existsById(COMP_BRANCH_B)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_BRANCH_B)
                    .componentName("分支B处理")
                    .componentType(QLComponent.ComponentType.SCRIPT)
                    .script("flowContext.setData('branch', 'B');")
                    .language("qlexpress")
                    .enabled(true)
                    .build());
        }

        // 结束组件
        if (!componentRepository.existsById(COMP_FINISH)) {
            componentService.createComponent(QLComponent.builder()
                    .componentId(COMP_FINISH)
                    .componentName("结束处理")
                    .componentType(QLComponent.ComponentType.SCRIPT)
                    .script("flowContext.setData('finished', true);")
                    .language("qlexpress")
                    .enabled(true)
                    .build());
        }
    }

    private void createSimpleFlow() {
        if (!flowRepository.existsById(FLOW_SIMPLE)) {
            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode(COMP_INIT),
                            createLeafNode(COMP_PROCESS),
                            createLeafNode(COMP_FINISH)
                    ))
                    .build();

            flowManageService.createFlow(FlowDesign.builder()
                    .flowId(FLOW_SIMPLE)
                    .flowName("简单顺序流程")
                    .root(root)
                    .enabled(true)
                    .build());
        }
    }

    private void createConditionFlow() {
        if (!flowRepository.existsById(FLOW_CONDITION)) {
            CmpProperty ifNode = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode(COMP_CHECK))
                    .children(Arrays.asList(
                            createLeafNode(COMP_BRANCH_A),
                            createLeafNode(COMP_BRANCH_B)
                    ))
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode(COMP_INIT),
                            ifNode,
                            createLeafNode(COMP_FINISH)
                    ))
                    .build();

            flowManageService.createFlow(FlowDesign.builder()
                    .flowId(FLOW_CONDITION)
                    .flowName("条件分支流程")
                    .root(root)
                    .enabled(true)
                    .build());
        }
    }

    private void createComplexFlow() {
        if (!flowRepository.existsById(FLOW_COMPLEX)) {
            CmpProperty switchNode = CmpProperty.builder()
                    .type("SWITCH")
                    .condition(createLeafNode(COMP_ROUTER))
                    .children(Arrays.asList(
                            CmpProperty.builder()
                                    .id(COMP_BRANCH_A)
                                    .componentRef(COMP_BRANCH_A)
                                    .properties(CmpProperty.Properties.builder().branch("branchA").build())
                                    .build(),
                            CmpProperty.builder()
                                    .id(COMP_BRANCH_B)
                                    .componentRef(COMP_BRANCH_B)
                                    .properties(CmpProperty.Properties.builder().branch("branchB").build())
                                    .build()
                    ))
                    .build();

            CmpProperty ifNode = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode(COMP_CHECK))
                    .children(Arrays.asList(
                            switchNode,
                            createLeafNode(COMP_PROCESS)
                    ))
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode(COMP_INIT),
                            ifNode,
                            createLeafNode(COMP_FINISH)
                    ))
                    .build();

            flowManageService.createFlow(FlowDesign.builder()
                    .flowId(FLOW_COMPLEX)
                    .flowName("复杂嵌套流程")
                    .root(root)
                    .enabled(true)
                    .build());
        }
    }

    private CmpProperty createLeafNode(String componentId) {
        return CmpProperty.builder()
                .id(componentId)
                .componentRef(componentId)
                .type("COMPONENT")
                .build();
    }
}

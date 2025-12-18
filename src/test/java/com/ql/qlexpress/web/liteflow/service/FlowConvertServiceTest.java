package com.ql.qlexpress.web.liteflow.service;

import com.ql.qlexpress.web.liteflow.model.CmpProperty;
import com.ql.qlexpress.web.liteflow.model.ConvertResult;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import com.ql.qlexpress.web.liteflow.repository.QLComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 流程转换服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("流程转换服务测试")
class FlowConvertServiceTest {

    @Mock
    private QLComponentRepository componentRepository;

    @InjectMocks
    private FlowConvertService convertService;

    @BeforeEach
    void setUp() {
        // 设置常用组件的 mock
        when(componentRepository.findById("componentA")).thenReturn(Optional.of(
                createComponent("componentA", "组件A", QLComponent.ComponentType.SCRIPT)));
        when(componentRepository.findById("componentB")).thenReturn(Optional.of(
                createComponent("componentB", "组件B", QLComponent.ComponentType.SCRIPT)));
        when(componentRepository.findById("componentC")).thenReturn(Optional.of(
                createComponent("componentC", "组件C", QLComponent.ComponentType.SCRIPT)));
        when(componentRepository.findById("checkCondition")).thenReturn(Optional.of(
                createComponent("checkCondition", "条件检查", QLComponent.ComponentType.BOOLEAN)));
        when(componentRepository.findById("switchRouter")).thenReturn(Optional.of(
                createComponent("switchRouter", "路由选择", QLComponent.ComponentType.SWITCH)));
        when(componentRepository.existsById("componentA")).thenReturn(true);
        when(componentRepository.existsById("componentB")).thenReturn(true);
        when(componentRepository.existsById("componentC")).thenReturn(true);
        when(componentRepository.existsById("checkCondition")).thenReturn(true);
        when(componentRepository.existsById("switchRouter")).thenReturn(true);
    }

    // ==================== THEN 表达式测试 ====================

    @Nested
    @DisplayName("THEN 表达式生成")
    class ThenExpressionTests {

        @Test
        @DisplayName("简单 THEN 表达式")
        void testSimpleThen() {
            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB"),
                            createLeafNode("componentC")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("THEN(componentA, componentB, componentC)", el);
        }

        @Test
        @DisplayName("嵌套 THEN 表达式")
        void testNestedThen() {
            CmpProperty innerThen = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentB"),
                            createLeafNode("componentC")
                    ))
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            innerThen
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("THEN(componentA, THEN(componentB, componentC))", el);
        }

        @Test
        @DisplayName("带 ID 修饰符的 THEN")
        void testThenWithId() {
            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(createLeafNode("componentA")))
                    .properties(CmpProperty.Properties.builder().id("step1").build())
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("THEN(componentA).id(\"step1\")", el);
        }
    }

    // ==================== WHEN 表达式测试 ====================

    @Nested
    @DisplayName("WHEN 表达式生成")
    class WhenExpressionTests {

        @Test
        @DisplayName("简单 WHEN 表达式")
        void testSimpleWhen() {
            CmpProperty root = CmpProperty.builder()
                    .type("WHEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("WHEN(componentA, componentB)", el);
        }

        @Test
        @DisplayName("带超时的 WHEN 表达式")
        void testWhenWithTimeout() {
            CmpProperty root = CmpProperty.builder()
                    .type("WHEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .properties(CmpProperty.Properties.builder().maxWaitSeconds(10).build())
                    .build();

            String el = convertService.generateELOnly(root);

            assertTrue(el.contains("WHEN(componentA, componentB)"));
            assertTrue(el.contains(".maxWaitSeconds(10)"));
        }

        @Test
        @DisplayName("带 ignoreError 的 WHEN 表达式")
        void testWhenWithIgnoreError() {
            CmpProperty root = CmpProperty.builder()
                    .type("WHEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .properties(CmpProperty.Properties.builder().ignoreError(true).build())
                    .build();

            String el = convertService.generateELOnly(root);

            assertTrue(el.contains(".ignoreError(true)"));
        }
    }

    // ==================== IF 表达式测试 ====================

    @Nested
    @DisplayName("IF 表达式生成")
    class IfExpressionTests {

        @Test
        @DisplayName("简单 IF 表达式 (只有 true 分支)")
        void testSimpleIf() {
            CmpProperty root = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode("checkCondition"))
                    .children(Arrays.asList(
                            createLeafNode("componentA")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("IF(checkCondition, componentA)", el);
        }

        @Test
        @DisplayName("完整 IF-ELSE 表达式")
        void testIfElse() {
            CmpProperty root = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode("checkCondition"))
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("IF(checkCondition, componentA, componentB)", el);
        }

        @Test
        @DisplayName("嵌套 IF 表达式")
        void testNestedIf() {
            CmpProperty innerIf = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode("checkCondition"))
                    .children(Arrays.asList(
                            createLeafNode("componentB"),
                            createLeafNode("componentC")
                    ))
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            innerIf
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("THEN(componentA, IF(checkCondition, componentB, componentC))", el);
        }

        @Test
        @DisplayName("IF 配合 THEN")
        void testIfWithThen() {
            CmpProperty thenBranch = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode("checkCondition"))
                    .children(Arrays.asList(
                            thenBranch,
                            createLeafNode("componentC")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("IF(checkCondition, THEN(componentA, componentB), componentC)", el);
        }
    }

    // ==================== SWITCH 表达式测试 ====================

    @Nested
    @DisplayName("SWITCH 表达式生成")
    class SwitchExpressionTests {

        @Test
        @DisplayName("简单 SWITCH 表达式")
        void testSimpleSwitch() {
            CmpProperty branch1 = CmpProperty.builder()
                    .id("componentA")
                    .properties(CmpProperty.Properties.builder().branch("A").build())
                    .build();
            CmpProperty branch2 = CmpProperty.builder()
                    .id("componentB")
                    .properties(CmpProperty.Properties.builder().branch("B").build())
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("SWITCH")
                    .condition(createLeafNode("switchRouter"))
                    .children(Arrays.asList(branch1, branch2))
                    .build();

            String el = convertService.generateELOnly(root);

            assertTrue(el.startsWith("SWITCH(switchRouter).to("));
            assertTrue(el.contains("componentA"));
            assertTrue(el.contains("componentB"));
            assertTrue(el.contains(".id("));
        }
    }

    // ==================== FOR 表达式测试 ====================

    @Nested
    @DisplayName("FOR 表达式生成")
    class ForExpressionTests {

        @Test
        @DisplayName("简单 FOR 表达式")
        void testSimpleFor() {
            CmpProperty root = CmpProperty.builder()
                    .type("FOR")
                    .children(Arrays.asList(createLeafNode("componentA")))
                    .properties(CmpProperty.Properties.builder().loopCount(5).build())
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("FOR(5).DO(componentA)", el);
        }
    }

    // ==================== AND/OR/NOT 表达式测试 ====================

    @Nested
    @DisplayName("逻辑表达式生成")
    class LogicalExpressionTests {

        @Test
        @DisplayName("AND 表达式")
        void testAnd() {
            CmpProperty root = CmpProperty.builder()
                    .type("AND")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("AND(componentA, componentB)", el);
        }

        @Test
        @DisplayName("OR 表达式")
        void testOr() {
            CmpProperty root = CmpProperty.builder()
                    .type("OR")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("OR(componentA, componentB)", el);
        }

        @Test
        @DisplayName("NOT 表达式")
        void testNot() {
            CmpProperty root = CmpProperty.builder()
                    .type("NOT")
                    .children(Arrays.asList(createLeafNode("componentA")))
                    .build();

            String el = convertService.generateELOnly(root);

            assertEquals("NOT(componentA)", el);
        }
    }

    // ==================== 完整流程转换测试 ====================

    @Nested
    @DisplayName("完整流程转换")
    class FullConversionTests {

        @Test
        @DisplayName("转换完整流程")
        void testFullConversion() {
            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            CmpProperty.builder()
                                    .type("IF")
                                    .condition(createLeafNode("checkCondition"))
                                    .children(Arrays.asList(
                                            createLeafNode("componentB"),
                                            createLeafNode("componentC")
                                    ))
                                    .build()
                    ))
                    .build();

            FlowDesign flow = FlowDesign.builder()
                    .flowId("testFlow")
                    .flowName("测试流程")
                    .root(root)
                    .build();

            ConvertResult result = convertService.convert(flow);

            assertTrue(result.isSuccess());
            assertEquals("testFlow", result.getFlowId());
            assertNotNull(result.getEl());
            assertEquals("THEN(componentA, IF(checkCondition, componentB, componentC))", result.getEl());
            assertTrue(result.getUsedComponentIds().contains("componentA"));
            assertTrue(result.getUsedComponentIds().contains("checkCondition"));
        }

        @Test
        @DisplayName("转换复杂流程 - 开门示例")
        void testComplexFlowConversion() {
            // 模拟开门流程: THEN(init, getDoor, IF(checkWatch, THEN(IF(check4G, 4gOpen, normalOpen)), directOpen), playAudio)

            CmpProperty innerIf = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode("checkCondition"))
                    .children(Arrays.asList(
                            createLeafNode("componentB"),
                            createLeafNode("componentC")
                    ))
                    .build();

            CmpProperty thenBranch = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(innerIf))
                    .build();

            CmpProperty outerIf = CmpProperty.builder()
                    .type("IF")
                    .condition(createLeafNode("checkCondition"))
                    .children(Arrays.asList(
                            thenBranch,
                            createLeafNode("componentA")
                    ))
                    .build();

            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            outerIf,
                            createLeafNode("componentB")
                    ))
                    .build();

            FlowDesign flow = FlowDesign.builder()
                    .flowId("openDoor")
                    .flowName("开门流程")
                    .root(root)
                    .build();

            ConvertResult result = convertService.convert(flow);

            assertTrue(result.isSuccess());
            assertNotNull(result.getEl());
            assertTrue(result.getEl().contains("THEN"));
            assertTrue(result.getEl().contains("IF"));
        }
    }

    // ==================== 验证测试 ====================

    @Nested
    @DisplayName("流程验证")
    class ValidationTests {

        @Test
        @DisplayName("验证有效流程")
        void testValidFlow() {
            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("componentB")
                    ))
                    .build();

            FlowDesign flow = FlowDesign.builder()
                    .flowId("validFlow")
                    .flowName("有效流程")
                    .root(root)
                    .build();

            FlowConvertService.ValidationResult result = convertService.validate(flow);

            assertTrue(result.isValid());
            assertTrue(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("验证缺少条件的 IF")
        void testIfWithoutCondition() {
            CmpProperty root = CmpProperty.builder()
                    .type("IF")
                    .children(Arrays.asList(createLeafNode("componentA")))
                    .build();

            FlowDesign flow = FlowDesign.builder()
                    .flowId("invalidFlow")
                    .flowName("无效流程")
                    .root(root)
                    .build();

            FlowConvertService.ValidationResult result = convertService.validate(flow);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("IF") && e.contains("条件")));
        }

        @Test
        @DisplayName("验证引用不存在的组件")
        void testNonExistentComponent() {
            when(componentRepository.existsById("nonExistent")).thenReturn(false);

            CmpProperty root = CmpProperty.builder()
                    .type("THEN")
                    .children(Arrays.asList(
                            createLeafNode("componentA"),
                            createLeafNode("nonExistent")
                    ))
                    .build();

            FlowDesign flow = FlowDesign.builder()
                    .flowId("testFlow")
                    .flowName("测试流程")
                    .root(root)
                    .build();

            FlowConvertService.ValidationResult result = convertService.validate(flow);

            assertFalse(result.isValid());
            assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("nonExistent")));
        }
    }

    // ==================== 辅助方法 ====================

    private CmpProperty createLeafNode(String componentId) {
        return CmpProperty.builder()
                .id(componentId)
                .componentRef(componentId)
                .type("COMPONENT")
                .build();
    }

    private QLComponent createComponent(String id, String name, QLComponent.ComponentType type) {
        return QLComponent.builder()
                .componentId(id)
                .componentName(name)
                .componentType(type)
                .script("return true;")
                .language("qlexpress")
                .build();
    }
}

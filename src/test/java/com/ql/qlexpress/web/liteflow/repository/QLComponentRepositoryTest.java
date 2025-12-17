package com.ql.qlexpress.web.liteflow.repository;

import com.ql.qlexpress.web.liteflow.model.QLComponent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QLComponent 仓储单元测试
 */
@DisplayName("QLComponent 仓储测试")
class QLComponentRepositoryTest {

    private QLComponentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new QLComponentRepository();
        // 禁用自动持久化
        ReflectionTestUtils.setField(repository, "autoPersist", false);
        // 设置测试存储路径
        ReflectionTestUtils.setField(repository, "storagePath", "target/test-components.json");
    }

    // ==================== 基础 CRUD 测试 ====================

    @Test
    @DisplayName("保存组件")
    void testSave() {
        QLComponent component = createTestComponent("test1", "测试组件", "基础");

        QLComponent saved = repository.save(component);

        assertNotNull(saved);
        assertEquals("test1", saved.getComponentId());
        assertTrue(repository.existsById("test1"));
    }

    @Test
    @DisplayName("根据ID查找组件")
    void testFindById() {
        QLComponent component = createTestComponent("test2", "测试组件2", "基础");
        repository.save(component);

        Optional<QLComponent> found = repository.findById("test2");

        assertTrue(found.isPresent());
        assertEquals("测试组件2", found.get().getComponentName());
    }

    @Test
    @DisplayName("查找不存在的组件")
    void testFindByIdNotFound() {
        Optional<QLComponent> found = repository.findById("not_exist");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("更新组件")
    void testUpdate() {
        QLComponent component = createTestComponent("test3", "原名称", "基础");
        repository.save(component);

        // 更新名称
        component.setComponentName("新名称");
        repository.save(component);

        Optional<QLComponent> updated = repository.findById("test3");
        assertTrue(updated.isPresent());
        assertEquals("新名称", updated.get().getComponentName());
    }

    @Test
    @DisplayName("删除组件")
    void testDelete() {
        QLComponent component = createTestComponent("test4", "测试组件", "基础");
        repository.save(component);
        assertTrue(repository.existsById("test4"));

        repository.deleteById("test4");

        assertFalse(repository.existsById("test4"));
    }

    @Test
    @DisplayName("获取所有组件")
    void testFindAll() {
        repository.save(createTestComponent("comp1", "组件1", "基础"));
        repository.save(createTestComponent("comp2", "组件2", "状态检查"));
        repository.save(createTestComponent("comp3", "组件3", "路由"));

        List<QLComponent> all = repository.findAll();

        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("统计组件数量")
    void testCount() {
        repository.save(createTestComponent("c1", "组件1", "基础"));
        repository.save(createTestComponent("c2", "组件2", "基础"));

        assertEquals(2, repository.count());
    }

    // ==================== 分类索引测试 ====================

    @Test
    @DisplayName("根据分类查找组件")
    void testFindByCategory() {
        repository.save(createTestComponent("base1", "基础组件1", "基础"));
        repository.save(createTestComponent("base2", "基础组件2", "基础"));
        repository.save(createTestComponent("check1", "检查组件1", "状态检查"));

        List<QLComponent> baseComponents = repository.findByCategory("基础");
        List<QLComponent> checkComponents = repository.findByCategory("状态检查");

        assertEquals(2, baseComponents.size());
        assertEquals(1, checkComponents.size());
    }

    @Test
    @DisplayName("获取所有分类")
    void testGetAllCategories() {
        repository.save(createTestComponent("c1", "组件1", "基础"));
        repository.save(createTestComponent("c2", "组件2", "状态检查"));
        repository.save(createTestComponent("c3", "组件3", "路由"));

        Set<String> categories = repository.getAllCategories();

        assertEquals(3, categories.size());
        assertTrue(categories.contains("基础"));
        assertTrue(categories.contains("状态检查"));
        assertTrue(categories.contains("路由"));
    }

    @Test
    @DisplayName("分类统计")
    void testCountByCategory() {
        repository.save(createTestComponent("c1", "组件1", "基础"));
        repository.save(createTestComponent("c2", "组件2", "基础"));
        repository.save(createTestComponent("c3", "组件3", "基础"));
        repository.save(createTestComponent("c4", "组件4", "状态检查"));

        Map<String, Integer> stats = repository.countByCategory();

        assertEquals(3, stats.get("基础").intValue());
        assertEquals(1, stats.get("状态检查").intValue());
    }

    @Test
    @DisplayName("更新组件分类后索引更新")
    void testCategoryIndexUpdateOnChange() {
        QLComponent component = createTestComponent("comp", "组件", "分类A");
        repository.save(component);

        assertEquals(1, repository.findByCategory("分类A").size());

        // 创建新的组件对象来模拟更新操作（category 不同）
        QLComponent updatedComponent = QLComponent.builder()
                .componentId("comp")
                .componentName("组件")
                .category("分类B")
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("return 1;")
                .language("qlexpress")
                .build();
        repository.save(updatedComponent);

        assertEquals(0, repository.findByCategory("分类A").size());
        assertEquals(1, repository.findByCategory("分类B").size());
    }

    // ==================== 类型查询测试 ====================

    @Test
    @DisplayName("根据组件类型查找")
    void testFindByComponentType() {
        repository.save(createTestComponentWithType("s1", "脚本1", QLComponent.ComponentType.SCRIPT));
        repository.save(createTestComponentWithType("b1", "布尔1", QLComponent.ComponentType.BOOLEAN));
        repository.save(createTestComponentWithType("b2", "布尔2", QLComponent.ComponentType.BOOLEAN));
        repository.save(createTestComponentWithType("sw1", "路由1", QLComponent.ComponentType.SWITCH));

        List<QLComponent> scripts = repository.findByComponentType(QLComponent.ComponentType.SCRIPT);
        List<QLComponent> booleans = repository.findByComponentType(QLComponent.ComponentType.BOOLEAN);
        List<QLComponent> switches = repository.findByComponentType(QLComponent.ComponentType.SWITCH);

        assertEquals(1, scripts.size());
        assertEquals(2, booleans.size());
        assertEquals(1, switches.size());
    }

    @Test
    @DisplayName("根据启用状态查找")
    void testFindByEnabled() {
        QLComponent enabled1 = createTestComponent("e1", "启用1", "基础");
        enabled1.setEnabled(true);
        repository.save(enabled1);

        QLComponent enabled2 = createTestComponent("e2", "启用2", "基础");
        enabled2.setEnabled(true);
        repository.save(enabled2);

        QLComponent disabled = createTestComponent("d1", "禁用1", "基础");
        disabled.setEnabled(false);
        repository.save(disabled);

        List<QLComponent> enabledList = repository.findByEnabled(true);
        List<QLComponent> disabledList = repository.findByEnabled(false);

        assertEquals(2, enabledList.size());
        assertEquals(1, disabledList.size());
    }

    // ==================== 批量查询测试 ====================

    @Test
    @DisplayName("根据多个ID查找")
    void testFindByIds() {
        repository.save(createTestComponent("c1", "组件1", "基础"));
        repository.save(createTestComponent("c2", "组件2", "基础"));
        repository.save(createTestComponent("c3", "组件3", "基础"));

        List<QLComponent> found = repository.findByIds(Arrays.asList("c1", "c3", "not_exist"));

        assertEquals(2, found.size());
    }

    @Test
    @DisplayName("搜索组件")
    void testSearch() {
        repository.save(createTestComponent("door1", "开门组件", "门操作"));
        repository.save(createTestComponent("door2", "关门组件", "门操作"));
        repository.save(createTestComponent("audio1", "播放语音", "音频"));

        List<QLComponent> doorResults = repository.search("门");
        List<QLComponent> audioResults = repository.search("语音");

        assertEquals(2, doorResults.size());
        assertEquals(1, audioResults.size());
    }

    @Test
    @DisplayName("搜索不区分大小写")
    void testSearchCaseInsensitive() {
        repository.save(createTestComponent("test", "TestComponent", "基础"));

        List<QLComponent> results = repository.search("testcomponent");

        assertEquals(1, results.size());
    }

    // ==================== 批量保存测试 ====================

    @Test
    @DisplayName("批量保存组件")
    void testSaveAll() {
        List<QLComponent> components = Arrays.asList(
                createTestComponent("batch1", "批量1", "基础"),
                createTestComponent("batch2", "批量2", "基础"),
                createTestComponent("batch3", "批量3", "状态检查")
        );

        List<QLComponent> saved = repository.saveAll(components);

        assertEquals(3, saved.size());
        assertEquals(3, repository.count());
    }

    // ==================== 删除所有测试 ====================

    @Test
    @DisplayName("删除所有组件")
    void testDeleteAll() {
        repository.save(createTestComponent("c1", "组件1", "基础"));
        repository.save(createTestComponent("c2", "组件2", "状态检查"));
        assertEquals(2, repository.count());

        repository.deleteAll();

        assertEquals(0, repository.count());
        assertTrue(repository.getAllCategories().isEmpty());
    }

    // ==================== 辅助方法 ====================

    private QLComponent createTestComponent(String id, String name, String category) {
        return QLComponent.builder()
                .componentId(id)
                .componentName(name)
                .category(category)
                .componentType(QLComponent.ComponentType.SCRIPT)
                .script("return true;")
                .language("qlexpress")
                .enabled(true)
                .build();
    }

    private QLComponent createTestComponentWithType(String id, String name, QLComponent.ComponentType type) {
        return QLComponent.builder()
                .componentId(id)
                .componentName(name)
                .category("测试")
                .componentType(type)
                .script("return true;")
                .language("qlexpress")
                .enabled(true)
                .build();
    }
}

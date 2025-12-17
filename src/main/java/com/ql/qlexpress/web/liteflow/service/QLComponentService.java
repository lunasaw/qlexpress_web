package com.ql.qlexpress.web.liteflow.service;

import com.ql.qlexpress.web.exception.BusinessException;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import com.ql.qlexpress.web.liteflow.repository.QLComponentRepository;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * QLExpress 组件管理服务
 * 提供组件 CRUD、验证、测试等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QLComponentService {

    private final QLComponentRepository componentRepository;
    private final LiteFlowDynamicService liteFlowDynamicService;

    private final Express4Runner expressRunner = new Express4Runner(InitOptions.builder().build());

    // ==================== 组件 CRUD ====================

    /**
     * 创建组件
     */
    public QLComponent createComponent(QLComponent component) {
        // 检查 ID 是否已存在
        if (componentRepository.existsById(component.getComponentId())) {
            throw new BusinessException("COMPONENT_ERROR", "组件ID已存在: " + component.getComponentId());
        }

        // 验证脚本语法
        validateScript(component);

        // 保存组件
        QLComponent saved = componentRepository.save(component);

        // 同步到 LiteFlow
        syncToLiteFlow(saved);

        log.info("创建组件成功: componentId={}, type={}", saved.getComponentId(), saved.getComponentType());
        return saved;
    }

    /**
     * 更新组件
     */
    public QLComponent updateComponent(String componentId, QLComponent component) {
        // 检查组件是否存在
        QLComponent existing = componentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException("COMPONENT_NOT_FOUND", "组件不存在: " + componentId));

        // 确保 ID 一致
        component.setComponentId(componentId);

        // 验证脚本语法
        validateScript(component);

        // 保存更新
        QLComponent saved = componentRepository.save(component);

        // 同步到 LiteFlow
        syncToLiteFlow(saved);

        log.info("更新组件成功: componentId={}", componentId);
        return saved;
    }

    /**
     * 获取组件详情
     */
    public QLComponent getComponent(String componentId) {
        return componentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException("COMPONENT_NOT_FOUND", "组件不存在: " + componentId));
    }

    /**
     * 获取组件 (可选)
     */
    public Optional<QLComponent> findComponent(String componentId) {
        return componentRepository.findById(componentId);
    }

    /**
     * 删除组件
     */
    public void deleteComponent(String componentId) {
        // 检查组件是否存在
        if (!componentRepository.existsById(componentId)) {
            throw new BusinessException("COMPONENT_NOT_FOUND", "组件不存在: " + componentId);
        }

        // 删除组件
        componentRepository.deleteById(componentId);

        log.info("删除组件成功: componentId={}", componentId);
    }

    /**
     * 获取组件列表
     */
    public List<QLComponent> listComponents(String category, QLComponent.ComponentType componentType, Boolean enabled) {
        List<QLComponent> components;

        // 先按分类过滤
        if (category != null && !category.isEmpty()) {
            components = componentRepository.findByCategory(category);
        } else {
            components = componentRepository.findAll();
        }

        // 再按类型过滤
        if (componentType != null) {
            components = components.stream()
                    .filter(c -> c.getComponentType() == componentType)
                    .collect(Collectors.toList());
        }

        // 最后按启用状态过滤
        if (enabled != null) {
            components = components.stream()
                    .filter(c -> c.isEnabled() == enabled)
                    .collect(Collectors.toList());
        }

        return components;
    }

    /**
     * 搜索组件
     */
    public List<QLComponent> searchComponents(String keyword) {
        return componentRepository.search(keyword);
    }

    /**
     * 批量创建组件
     */
    public List<QLComponent> batchCreateComponents(List<QLComponent> components) {
        List<QLComponent> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (QLComponent component : components) {
            try {
                results.add(createComponent(component));
            } catch (Exception e) {
                errors.add(component.getComponentId() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            log.warn("批量创建组件部分失败: {}", errors);
        }

        return results;
    }

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类
     */
    public Set<String> getAllCategories() {
        return componentRepository.getAllCategories();
    }

    /**
     * 获取分类统计
     */
    public Map<String, Integer> getCategoryStats() {
        return componentRepository.countByCategory();
    }

    // ==================== 脚本验证 ====================

    /**
     * 验证组件脚本
     */
    public ScriptValidationResult validateScript(QLComponent component) {
        ScriptValidationResult result = new ScriptValidationResult();
        result.setComponentId(component.getComponentId());

        String script = component.getScript();
        if (script == null || script.trim().isEmpty()) {
            result.setValid(false);
            result.addError("脚本内容不能为空");
            return result;
        }

        try {
            // 使用 QLExpress 进行语法检查
            expressRunner.parseToSyntaxTree(script);
            result.setValid(true);
            result.setMessage("脚本语法检查通过");
        } catch (Exception e) {
            result.setValid(false);
            result.addError("脚本语法错误: " + e.getMessage());
        }

        // 检查组件类型相关的返回值
        validateComponentTypeConstraints(component, result);

        return result;
    }

    /**
     * 单独验证脚本语法
     */
    public ScriptValidationResult validateScriptOnly(String script, String language) {
        ScriptValidationResult result = new ScriptValidationResult();

        if (script == null || script.trim().isEmpty()) {
            result.setValid(false);
            result.addError("脚本内容不能为空");
            return result;
        }

        try {
            if ("qlexpress".equalsIgnoreCase(language) || language == null) {
                expressRunner.parseToSyntaxTree(script);
            }
            result.setValid(true);
            result.setMessage("脚本语法检查通过");
        } catch (Exception e) {
            result.setValid(false);
            result.addError("脚本语法错误: " + e.getMessage());
        }

        return result;
    }

    private void validateComponentTypeConstraints(QLComponent component, ScriptValidationResult result) {
        String script = component.getScript().toLowerCase();

        switch (component.getComponentType()) {
            case BOOLEAN:
                // BOOLEAN 组件应该有 return 语句，返回 true/false
                if (!script.contains("return")) {
                    result.addWarning("BOOLEAN 组件建议包含 return 语句以返回布尔值");
                }
                break;
            case SWITCH:
                // SWITCH 组件应该有 return 语句，返回分支标识
                if (!script.contains("return")) {
                    result.addWarning("SWITCH 组件建议包含 return 语句以返回分支标识");
                }
                // 检查是否定义了分支
                if (component.getSwitchBranches() == null || component.getSwitchBranches().isEmpty()) {
                    result.addWarning("SWITCH 组件建议定义分支列表 (switchBranches)");
                }
                break;
            case SCRIPT:
                // 普通脚本可以有或没有 return
                break;
        }
    }

    // ==================== 组件测试 ====================

    /**
     * 测试组件执行
     */
    public ComponentTestResult testComponent(String componentId, Map<String, Object> testParams) {
        QLComponent component = getComponent(componentId);
        return testComponentExecution(component, testParams);
    }

    /**
     * 测试组件执行 (不需要先保存)
     */
    public ComponentTestResult testComponentExecution(QLComponent component, Map<String, Object> testParams) {
        ComponentTestResult result = new ComponentTestResult();
        result.setComponentId(component.getComponentId());

        long startTime = System.currentTimeMillis();

        try {
            // 先验证脚本
            ScriptValidationResult validation = validateScript(component);
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setErrorMessage("脚本验证失败: " + String.join(", ", validation.getErrors()));
                return result;
            }

            // 准备测试上下文
            Map<String, Object> context = new HashMap<>();
            if (testParams != null) {
                context.putAll(testParams);
            }

            // 模拟 flowContext
            context.put("flowContext", new TestFlowContext(context));

            // 执行脚本
            QLOptions qlOptions = QLOptions.builder().build();
            QLResult qlResult = expressRunner.execute(component.getScript(), context, qlOptions);

            result.setSuccess(true);
            result.setReturnValue(qlResult.getResult());
            result.setContext(context);
            result.setExecutionTime(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("执行失败: " + e.getMessage());
            result.setExecutionTime(System.currentTimeMillis() - startTime);
            log.error("测试组件执行失败: componentId={}", component.getComponentId(), e);
        }

        return result;
    }

    // ==================== LiteFlow 同步 ====================

    /**
     * 同步组件到 LiteFlow
     */
    public void syncToLiteFlow(QLComponent component) {
        if (!component.isEnabled()) {
            log.debug("组件已禁用，跳过同步: componentId={}", component.getComponentId());
            return;
        }

        try {
            liteFlowDynamicService.addScriptNode(
                    component.getComponentId(),
                    component.getComponentName(),
                    component.getScript(),
                    component.toLiteFlowNodeType(),
                    component.getLanguage()
            );
            log.debug("组件同步到 LiteFlow 成功: componentId={}", component.getComponentId());
        } catch (Exception e) {
            log.error("组件同步到 LiteFlow 失败: componentId={}", component.getComponentId(), e);
            throw new BusinessException("LITEFLOW_SYNC_ERROR", "同步到 LiteFlow 失败: " + e.getMessage());
        }
    }

    /**
     * 同步所有启用的组件到 LiteFlow
     */
    public int syncAllToLiteFlow() {
        List<QLComponent> enabledComponents = componentRepository.findByEnabled(true);
        int successCount = 0;

        for (QLComponent component : enabledComponents) {
            try {
                syncToLiteFlow(component);
                successCount++;
            } catch (Exception e) {
                log.error("同步组件失败: componentId={}", component.getComponentId(), e);
            }
        }

        log.info("同步所有组件到 LiteFlow 完成: {}/{}", successCount, enabledComponents.size());
        return successCount;
    }

    // ==================== 内部类 ====================

    /**
     * 脚本验证结果
     */
    @lombok.Data
    public static class ScriptValidationResult {
        private String componentId;
        private boolean valid = true;
        private String message;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
    }

    /**
     * 组件测试结果
     */
    @lombok.Data
    public static class ComponentTestResult {
        private String componentId;
        private boolean success;
        private Object returnValue;
        private Map<String, Object> context;
        private String errorMessage;
        private long executionTime;
    }

    /**
     * 测试用 FlowContext
     */
    private static class TestFlowContext extends HashMap<String, Object> {
        public TestFlowContext(Map<String, Object> initialData) {
            super(initialData);
        }

        @SuppressWarnings("unchecked")
        public <T> T getData(String key) {
            return (T) get(key);
        }

        public <T> T getData(String key, T defaultValue) {
            Object value = get(key);
            return value != null ? (T) value : defaultValue;
        }

        public TestFlowContext setData(String key, Object value) {
            put(key, value);
            return this;
        }

        public String getString(String key) {
            Object value = get(key);
            return value != null ? String.valueOf(value) : null;
        }

        public String getString(String key, String defaultValue) {
            Object value = get(key);
            return value != null ? String.valueOf(value) : defaultValue;
        }

        public Integer getInt(String key) {
            Object value = get(key);
            if (value == null) return null;
            if (value instanceof Number) return ((Number) value).intValue();
            return Integer.parseInt(String.valueOf(value));
        }

        public int getInt(String key, int defaultValue) {
            Integer value = getInt(key);
            return value != null ? value : defaultValue;
        }

        public Long getLong(String key) {
            Object value = get(key);
            if (value == null) return null;
            if (value instanceof Number) return ((Number) value).longValue();
            return Long.parseLong(String.valueOf(value));
        }

        public long getLong(String key, long defaultValue) {
            Long value = getLong(key);
            return value != null ? value : defaultValue;
        }

        public Boolean getBoolean(String key) {
            Object value = get(key);
            if (value == null) return null;
            if (value instanceof Boolean) return (Boolean) value;
            return Boolean.parseBoolean(String.valueOf(value));
        }

        public boolean getBoolean(String key, boolean defaultValue) {
            Boolean value = getBoolean(key);
            return value != null ? value : defaultValue;
        }
    }
}

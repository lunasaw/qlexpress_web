package com.ql.qlexpress.web.liteflow.controller;

import com.ql.qlexpress.web.liteflow.dto.*;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import com.ql.qlexpress.web.liteflow.service.QLComponentService;
import com.ql.qlexpress.web.model.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * QLExpress 组件管理控制器
 * 提供组件 CRUD、验证、测试等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/component")
@RequiredArgsConstructor
public class QLComponentController {

    private final QLComponentService componentService;

    // ==================== 组件 CRUD ====================

    /**
     * 创建组件
     */
    @PostMapping
    public ApiResponse<QLComponent> createComponent(@Validated @RequestBody ComponentRequest request) {
        try {
            QLComponent component = componentService.createComponent(request.toEntity());
            return ApiResponse.success(component);
        } catch (Exception e) {
            log.error("创建组件失败: componentId={}", request.getComponentId(), e);
            return ApiResponse.error("创建组件失败: " + e.getMessage());
        }
    }

    /**
     * 获取组件详情
     */
    @GetMapping("/{componentId}")
    public ApiResponse<QLComponent> getComponent(@PathVariable String componentId) {
        try {
            QLComponent component = componentService.getComponent(componentId);
            return ApiResponse.success(component);
        } catch (Exception e) {
            log.error("获取组件失败: componentId={}", componentId, e);
            return ApiResponse.error("获取组件失败: " + e.getMessage());
        }
    }

    /**
     * 更新组件
     */
    @PutMapping("/{componentId}")
    public ApiResponse<QLComponent> updateComponent(
            @PathVariable String componentId,
            @Validated @RequestBody ComponentRequest request) {
        try {
            QLComponent component = componentService.updateComponent(componentId, request.toEntity());
            return ApiResponse.success(component);
        } catch (Exception e) {
            log.error("更新组件失败: componentId={}", componentId, e);
            return ApiResponse.error("更新组件失败: " + e.getMessage());
        }
    }

    /**
     * 删除组件
     */
    @DeleteMapping("/{componentId}")
    public ApiResponse<String> deleteComponent(@PathVariable String componentId) {
        try {
            componentService.deleteComponent(componentId);
            return ApiResponse.success("组件删除成功: " + componentId);
        } catch (Exception e) {
            log.error("删除组件失败: componentId={}", componentId, e);
            return ApiResponse.error("删除组件失败: " + e.getMessage());
        }
    }

    /**
     * 获取组件列表
     */
    @GetMapping("/list")
    public ApiResponse<ComponentListResponse> listComponents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) QLComponent.ComponentType componentType,
            @RequestParam(required = false) Boolean enabled) {
        try {
            List<QLComponent> components = componentService.listComponents(category, componentType, enabled);
            Set<String> categories = componentService.getAllCategories();
            Map<String, Integer> categoryStats = componentService.getCategoryStats();

            List<ComponentListResponse.ComponentSummary> summaries = components.stream()
                    .map(ComponentListResponse.ComponentSummary::fromEntity)
                    .collect(Collectors.toList());

            ComponentListResponse response = ComponentListResponse.builder()
                    .total(summaries.size())
                    .categories(categories)
                    .components(summaries)
                    .categoryStats(categoryStats)
                    .build();

            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取组件列表失败", e);
            return ApiResponse.error("获取组件列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索组件
     */
    @GetMapping("/search")
    public ApiResponse<List<ComponentListResponse.ComponentSummary>> searchComponents(
            @RequestParam String keyword) {
        try {
            List<QLComponent> components = componentService.searchComponents(keyword);
            List<ComponentListResponse.ComponentSummary> summaries = components.stream()
                    .map(ComponentListResponse.ComponentSummary::fromEntity)
                    .collect(Collectors.toList());
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("搜索组件失败: keyword={}", keyword, e);
            return ApiResponse.error("搜索组件失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建组件
     */
    @PostMapping("/batch")
    public ApiResponse<List<QLComponent>> batchCreateComponents(@RequestBody List<ComponentRequest> requests) {
        try {
            List<QLComponent> components = requests.stream()
                    .map(ComponentRequest::toEntity)
                    .collect(Collectors.toList());
            List<QLComponent> created = componentService.batchCreateComponents(components);
            return ApiResponse.success(created);
        } catch (Exception e) {
            log.error("批量创建组件失败", e);
            return ApiResponse.error("批量创建组件失败: " + e.getMessage());
        }
    }

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类
     */
    @GetMapping("/category")
    public ApiResponse<Set<String>> getAllCategories() {
        try {
            Set<String> categories = componentService.getAllCategories();
            return ApiResponse.success(categories);
        } catch (Exception e) {
            log.error("获取分类失败", e);
            return ApiResponse.error("获取分类失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类统计
     */
    @GetMapping("/category/stats")
    public ApiResponse<Map<String, Integer>> getCategoryStats() {
        try {
            Map<String, Integer> stats = componentService.getCategoryStats();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取分类统计失败", e);
            return ApiResponse.error("获取分类统计失败: " + e.getMessage());
        }
    }

    // ==================== 脚本验证 ====================

    /**
     * 验证组件脚本
     */
    @PostMapping("/{componentId}/validate")
    public ApiResponse<QLComponentService.ScriptValidationResult> validateComponent(
            @PathVariable String componentId) {
        try {
            QLComponent component = componentService.getComponent(componentId);
            QLComponentService.ScriptValidationResult result = componentService.validateScript(component);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证组件失败: componentId={}", componentId, e);
            return ApiResponse.error("验证组件失败: " + e.getMessage());
        }
    }

    /**
     * 验证脚本语法 (不需要先保存组件)
     */
    @PostMapping("/validate")
    public ApiResponse<QLComponentService.ScriptValidationResult> validateScript(
            @Validated @RequestBody ScriptValidateRequest request) {
        try {
            QLComponentService.ScriptValidationResult result =
                    componentService.validateScriptOnly(request.getScript(), request.getLanguage());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证脚本失败", e);
            return ApiResponse.error("验证脚本失败: " + e.getMessage());
        }
    }

    // ==================== 组件测试 ====================

    /**
     * 测试组件执行
     */
    @PostMapping("/{componentId}/test")
    public ApiResponse<QLComponentService.ComponentTestResult> testComponent(
            @PathVariable String componentId,
            @RequestBody(required = false) ComponentTestRequest request) {
        try {
            Map<String, Object> params = request != null ? request.getParams() : null;
            QLComponentService.ComponentTestResult result = componentService.testComponent(componentId, params);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("测试组件失败: componentId={}", componentId, e);
            return ApiResponse.error("测试组件失败: " + e.getMessage());
        }
    }

    /**
     * 直接测试脚本 (不需要先保存组件)
     */
    @PostMapping("/test")
    public ApiResponse<QLComponentService.ComponentTestResult> testScript(
            @RequestBody ComponentTestRequest request) {
        try {
            QLComponent component = QLComponent.builder()
                    .componentId("test_" + System.currentTimeMillis())
                    .componentName("测试组件")
                    .componentType(QLComponent.ComponentType.SCRIPT)
                    .script(request.getScript())
                    .language(request.getLanguage())
                    .build();

            QLComponentService.ComponentTestResult result =
                    componentService.testComponentExecution(component, request.getParams());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("测试脚本失败", e);
            return ApiResponse.error("测试脚本失败: " + e.getMessage());
        }
    }

    // ==================== LiteFlow 同步 ====================

    /**
     * 同步单个组件到 LiteFlow
     */
    @PostMapping("/{componentId}/sync")
    public ApiResponse<String> syncComponent(@PathVariable String componentId) {
        try {
            QLComponent component = componentService.getComponent(componentId);
            componentService.syncToLiteFlow(component);
            return ApiResponse.success("组件同步成功: " + componentId);
        } catch (Exception e) {
            log.error("同步组件失败: componentId={}", componentId, e);
            return ApiResponse.error("同步组件失败: " + e.getMessage());
        }
    }

    /**
     * 同步所有组件到 LiteFlow
     */
    @PostMapping("/sync-all")
    public ApiResponse<String> syncAllComponents() {
        try {
            int count = componentService.syncAllToLiteFlow();
            return ApiResponse.success("同步完成: " + count + " 个组件");
        } catch (Exception e) {
            log.error("同步所有组件失败", e);
            return ApiResponse.error("同步所有组件失败: " + e.getMessage());
        }
    }
}

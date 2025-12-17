package com.ql.qlexpress.web.liteflow.controller;

import com.ql.qlexpress.web.liteflow.dto.*;
import com.ql.qlexpress.web.liteflow.model.ConvertResult;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import com.ql.qlexpress.web.liteflow.service.FlowConvertService;
import com.ql.qlexpress.web.liteflow.service.FlowManageService;
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
 * 流程管理控制器
 * 提供流程 CRUD、部署、执行等 API
 */
@Slf4j
@RestController
@RequestMapping("/api/flow")
@RequiredArgsConstructor
public class FlowController {

    private final FlowManageService flowManageService;
    private final FlowConvertService flowConvertService;

    // ==================== 流程 CRUD ====================

    /**
     * 创建流程
     */
    @PostMapping
    public ApiResponse<FlowDesign> createFlow(@Validated @RequestBody FlowRequest request) {
        try {
            FlowDesign flow = flowManageService.createFlow(request.toEntity());
            return ApiResponse.success(flow);
        } catch (Exception e) {
            log.error("创建流程失败: flowId={}", request.getFlowId(), e);
            return ApiResponse.error("创建流程失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程详情
     */
    @GetMapping("/{flowId}")
    public ApiResponse<FlowDesign> getFlow(@PathVariable String flowId) {
        try {
            FlowDesign flow = flowManageService.getFlow(flowId);
            return ApiResponse.success(flow);
        } catch (Exception e) {
            log.error("获取流程失败: flowId={}", flowId, e);
            return ApiResponse.error("获取流程失败: " + e.getMessage());
        }
    }

    /**
     * 更新流程
     */
    @PutMapping("/{flowId}")
    public ApiResponse<FlowDesign> updateFlow(
            @PathVariable String flowId,
            @Validated @RequestBody FlowRequest request) {
        try {
            FlowDesign flow = flowManageService.updateFlow(flowId, request.toEntity());
            return ApiResponse.success(flow);
        } catch (Exception e) {
            log.error("更新流程失败: flowId={}", flowId, e);
            return ApiResponse.error("更新流程失败: " + e.getMessage());
        }
    }

    /**
     * 删除流程
     */
    @DeleteMapping("/{flowId}")
    public ApiResponse<String> deleteFlow(@PathVariable String flowId) {
        try {
            flowManageService.deleteFlow(flowId);
            return ApiResponse.success("流程删除成功: " + flowId);
        } catch (Exception e) {
            log.error("删除流程失败: flowId={}", flowId, e);
            return ApiResponse.error("删除流程失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程列表
     */
    @GetMapping("/list")
    public ApiResponse<FlowListResponse> listFlows(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) FlowDesign.DeployStatus deployStatus) {
        try {
            List<FlowDesign> flows = flowManageService.listFlows(category, enabled, deployStatus);
            Set<String> categories = flowManageService.getAllCategories();
            Map<String, Integer> categoryStats = flowManageService.getCategoryStats();

            List<FlowListResponse.FlowSummary> summaries = flows.stream()
                    .map(FlowListResponse.FlowSummary::fromEntity)
                    .collect(Collectors.toList());

            FlowListResponse response = FlowListResponse.builder()
                    .total(summaries.size())
                    .categories(categories)
                    .flows(summaries)
                    .categoryStats(categoryStats)
                    .build();

            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取流程列表失败", e);
            return ApiResponse.error("获取流程列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索流程
     */
    @GetMapping("/search")
    public ApiResponse<List<FlowListResponse.FlowSummary>> searchFlows(@RequestParam String keyword) {
        try {
            List<FlowDesign> flows = flowManageService.searchFlows(keyword);
            List<FlowListResponse.FlowSummary> summaries = flows.stream()
                    .map(FlowListResponse.FlowSummary::fromEntity)
                    .collect(Collectors.toList());
            return ApiResponse.success(summaries);
        } catch (Exception e) {
            log.error("搜索流程失败: keyword={}", keyword, e);
            return ApiResponse.error("搜索流程失败: " + e.getMessage());
        }
    }

    // ==================== 流程转换与验证 ====================

    /**
     * 转换流程为 LiteFlow 配置
     */
    @PostMapping("/{flowId}/convert")
    public ApiResponse<ConvertResult> convertFlow(@PathVariable String flowId) {
        try {
            ConvertResult result = flowManageService.convertFlow(flowId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("转换流程失败: flowId={}", flowId, e);
            return ApiResponse.error("转换流程失败: " + e.getMessage());
        }
    }

    /**
     * 预览 EL 表达式 (不保存)
     */
    @PostMapping("/preview-el")
    public ApiResponse<String> previewEL(@Validated @RequestBody FlowRequest request) {
        try {
            String el = flowManageService.previewEL(request.toEntity());
            return ApiResponse.success(el);
        } catch (Exception e) {
            log.error("预览EL失败", e);
            return ApiResponse.error("预览EL失败: " + e.getMessage());
        }
    }

    /**
     * 验证流程
     */
    @PostMapping("/{flowId}/validate")
    public ApiResponse<FlowConvertService.ValidationResult> validateFlow(@PathVariable String flowId) {
        try {
            FlowConvertService.ValidationResult result = flowManageService.validateFlow(flowId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证流程失败: flowId={}", flowId, e);
            return ApiResponse.error("验证流程失败: " + e.getMessage());
        }
    }

    /**
     * 验证流程设计 (不需要先保存)
     */
    @PostMapping("/validate")
    public ApiResponse<FlowConvertService.ValidationResult> validateFlowDesign(
            @Validated @RequestBody FlowRequest request) {
        try {
            FlowConvertService.ValidationResult result =
                    flowManageService.validateFlowDesign(request.toEntity());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("验证流程设计失败", e);
            return ApiResponse.error("验证流程设计失败: " + e.getMessage());
        }
    }

    // ==================== 流程部署 ====================

    /**
     * 部署流程到 LiteFlow
     */
    @PostMapping("/{flowId}/deploy")
    public ApiResponse<FlowManageService.DeployResult> deployFlow(@PathVariable String flowId) {
        try {
            FlowManageService.DeployResult result = flowManageService.deployFlow(flowId);
            if (result.isSuccess()) {
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("部署流程失败: flowId={}", flowId, e);
            return ApiResponse.error("部署流程失败: " + e.getMessage());
        }
    }

    /**
     * 卸载流程
     */
    @PostMapping("/{flowId}/undeploy")
    public ApiResponse<String> undeployFlow(@PathVariable String flowId) {
        try {
            flowManageService.undeployFlow(flowId);
            return ApiResponse.success("卸载流程成功: " + flowId);
        } catch (Exception e) {
            log.error("卸载流程失败: flowId={}", flowId, e);
            return ApiResponse.error("卸载流程失败: " + e.getMessage());
        }
    }

    /**
     * 获取流程部署状态
     */
    @GetMapping("/{flowId}/status")
    public ApiResponse<FlowDesign.DeployStatus> getFlowStatus(@PathVariable String flowId) {
        try {
            FlowDesign flow = flowManageService.getFlow(flowId);
            return ApiResponse.success(flow.getDeployStatus());
        } catch (Exception e) {
            log.error("获取流程状态失败: flowId={}", flowId, e);
            return ApiResponse.error("获取流程状态失败: " + e.getMessage());
        }
    }

    /**
     * 重新部署所有流程
     */
    @PostMapping("/redeploy-all")
    public ApiResponse<String> redeployAllFlows() {
        try {
            int count = flowManageService.redeployAllFlows();
            return ApiResponse.success("重新部署完成: " + count + " 个流程");
        } catch (Exception e) {
            log.error("重新部署所有流程失败", e);
            return ApiResponse.error("重新部署所有流程失败: " + e.getMessage());
        }
    }

    // ==================== 流程执行 ====================

    /**
     * 执行流程
     */
    @PostMapping("/{flowId}/execute")
    public ApiResponse<FlowManageService.ExecuteResult> executeFlow(
            @PathVariable String flowId,
            @RequestBody(required = false) FlowExecuteRequest request) {
        try {
            Map<String, Object> params = request != null ? request.getParams() : null;
            FlowManageService.ExecuteResult result = flowManageService.executeFlow(flowId, params);
            if (result.isSuccess()) {
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("执行流程失败: flowId={}", flowId, e);
            return ApiResponse.error("执行流程失败: " + e.getMessage());
        }
    }

    /**
     * 直接执行流程设计 (不需要先保存和部署)
     */
    @PostMapping("/execute-direct")
    public ApiResponse<FlowManageService.ExecuteResult> executeFlowDirect(
            @RequestBody DirectExecuteRequest request) {
        try {
            FlowManageService.ExecuteResult result =
                    flowManageService.executeFlowDirect(request.getFlow().toEntity(), request.getParams());
            if (result.isSuccess()) {
                return ApiResponse.success(result);
            } else {
                return ApiResponse.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("直接执行流程失败", e);
            return ApiResponse.error("直接执行流程失败: " + e.getMessage());
        }
    }

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类
     */
    @GetMapping("/category")
    public ApiResponse<Set<String>> getAllCategories() {
        try {
            Set<String> categories = flowManageService.getAllCategories();
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
            Map<String, Integer> stats = flowManageService.getCategoryStats();
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("获取分类统计失败", e);
            return ApiResponse.error("获取分类统计失败: " + e.getMessage());
        }
    }

    // ==================== 请求类 ====================

    /**
     * 直接执行请求
     */
    @lombok.Data
    public static class DirectExecuteRequest {
        @javax.validation.Valid
        private FlowRequest flow;
        private Map<String, Object> params;
    }
}

package com.ql.qlexpress.web.liteflow.service;

import com.ql.qlexpress.web.exception.BusinessException;
import com.ql.qlexpress.web.liteflow.model.ConvertResult;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import com.ql.qlexpress.web.liteflow.model.CmpProperty;
import com.ql.qlexpress.web.liteflow.repository.FlowDesignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程管理服务
 * 提供流程 CRUD、部署、执行等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowManageService {

    private final FlowDesignRepository flowRepository;
    private final FlowConvertService flowConvertService;
    private final LiteFlowDynamicService liteFlowDynamicService;
    private final QLComponentService componentService;

    // ==================== 流程 CRUD ====================

    /**
     * 创建流程
     */
    public FlowDesign createFlow(FlowDesign flow) {
        // 检查 ID 是否已存在
        if (flowRepository.existsById(flow.getFlowId())) {
            throw new BusinessException("FLOW_ERROR", "流程ID已存在: " + flow.getFlowId());
        }

        // 验证流程
        FlowConvertService.ValidationResult validation = flowConvertService.validate(flow);
        if (!validation.isValid()) {
            throw new BusinessException("FLOW_VALIDATION_ERROR", "流程验证失败: " + String.join(", ", validation.getErrors()));
        }

        // 收集使用的组件ID
        Set<String> componentIds = new HashSet<>();
        collectComponentIds(flow.getRoot(), componentIds);
        flow.setUsedComponentIds(componentIds);

        // 设置元数据
        flow.setCreateTime(LocalDateTime.now());
        flow.setUpdateTime(LocalDateTime.now());
        flow.setDeployStatus(FlowDesign.DeployStatus.NOT_DEPLOYED);

        // 保存流程
        FlowDesign saved = flowRepository.save(flow);

        log.info("创建流程成功: flowId={}", saved.getFlowId());
        return saved;
    }

    /**
     * 更新流程
     */
    public FlowDesign updateFlow(String flowId, FlowDesign flow) {
        // 检查流程是否存在
        FlowDesign existing = flowRepository.findById(flowId)
                .orElseThrow(() -> new BusinessException("FLOW_NOT_FOUND", "流程不存在: " + flowId));

        // 确保 ID 一致
        flow.setFlowId(flowId);

        // 验证流程
        FlowConvertService.ValidationResult validation = flowConvertService.validate(flow);
        if (!validation.isValid()) {
            throw new BusinessException("FLOW_VALIDATION_ERROR", "流程验证失败: " + String.join(", ", validation.getErrors()));
        }

        // 收集使用的组件ID
        Set<String> componentIds = new HashSet<>();
        collectComponentIds(flow.getRoot(), componentIds);
        flow.setUsedComponentIds(componentIds);

        // 保持创建时间，更新修改时间
        flow.setCreateTime(existing.getCreateTime());
        flow.setUpdateTime(LocalDateTime.now());

        // 如果已部署，标记为已修改
        if (existing.getDeployStatus() == FlowDesign.DeployStatus.DEPLOYED) {
            flow.setDeployStatus(FlowDesign.DeployStatus.MODIFIED);
        }

        // 保存更新
        FlowDesign saved = flowRepository.save(flow);

        log.info("更新流程成功: flowId={}", flowId);
        return saved;
    }

    /**
     * 获取流程详情
     */
    public FlowDesign getFlow(String flowId) {
        return flowRepository.findById(flowId)
                .orElseThrow(() -> new BusinessException("FLOW_NOT_FOUND", "流程不存在: " + flowId));
    }

    /**
     * 获取流程 (可选)
     */
    public Optional<FlowDesign> findFlow(String flowId) {
        return flowRepository.findById(flowId);
    }

    /**
     * 删除流程
     */
    public void deleteFlow(String flowId) {
        // 检查流程是否存在
        FlowDesign flow = flowRepository.findById(flowId)
                .orElseThrow(() -> new BusinessException("FLOW_NOT_FOUND", "流程不存在: " + flowId));

        // 如果已部署，先卸载
        if (flow.getDeployStatus() == FlowDesign.DeployStatus.DEPLOYED) {
            try {
                liteFlowDynamicService.removeChain(flowId);
            } catch (Exception e) {
                log.warn("卸载流程失败: flowId={}", flowId, e);
            }
        }

        // 删除流程
        flowRepository.deleteById(flowId);

        log.info("删除流程成功: flowId={}", flowId);
    }

    /**
     * 获取流程列表
     */
    public List<FlowDesign> listFlows(String category, Boolean enabled, FlowDesign.DeployStatus deployStatus) {
        List<FlowDesign> flows;

        // 先按分类过滤
        if (category != null && !category.isEmpty()) {
            flows = flowRepository.findByCategory(category);
        } else {
            flows = flowRepository.findAll();
        }

        // 再按启用状态过滤
        if (enabled != null) {
            flows = flows.stream()
                    .filter(f -> f.isEnabled() == enabled)
                    .collect(Collectors.toList());
        }

        // 最后按部署状态过滤
        if (deployStatus != null) {
            flows = flows.stream()
                    .filter(f -> f.getDeployStatus() == deployStatus)
                    .collect(Collectors.toList());
        }

        return flows;
    }

    /**
     * 搜索流程
     */
    public List<FlowDesign> searchFlows(String keyword) {
        return flowRepository.search(keyword);
    }

    // ==================== 流程转换 ====================

    /**
     * 转换流程为 LiteFlow 配置
     */
    public ConvertResult convertFlow(String flowId) {
        FlowDesign flow = getFlow(flowId);
        return flowConvertService.convert(flow);
    }

    /**
     * 预览 EL 表达式 (不保存)
     */
    public String previewEL(FlowDesign flow) {
        return flowConvertService.generateELOnly(flow.getRoot());
    }

    /**
     * 验证流程
     */
    public FlowConvertService.ValidationResult validateFlow(String flowId) {
        FlowDesign flow = getFlow(flowId);
        return flowConvertService.validate(flow);
    }

    /**
     * 验证流程 (不需要先保存)
     */
    public FlowConvertService.ValidationResult validateFlowDesign(FlowDesign flow) {
        return flowConvertService.validate(flow);
    }

    // ==================== 流程部署 ====================

    /**
     * 部署流程到 LiteFlow
     */
    public DeployResult deployFlow(String flowId) {
        FlowDesign flow = getFlow(flowId);
        DeployResult result = new DeployResult();
        result.setFlowId(flowId);

        try {
            // 1. 转换流程
            ConvertResult convertResult = flowConvertService.convert(flow);
            if (!convertResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorMessage("转换失败: " + convertResult.getErrorMessage());
                return result;
            }

            // 2. 加载组件到 LiteFlow
            for (ConvertResult.NodeDefinition node : convertResult.getNodes()) {
                liteFlowDynamicService.addScriptNode(
                        node.getNodeId(),
                        node.getNodeName(),
                        node.getScript(),
                        node.getNodeType(),
                        node.getLanguage()
                );
            }

            // 3. 加载流程链到 LiteFlow
            liteFlowDynamicService.addOrUpdateChain(flowId, convertResult.getEl());

            // 4. 更新部署状态
            flow.setDeployStatus(FlowDesign.DeployStatus.DEPLOYED);
            flow.setLastDeployTime(LocalDateTime.now());
            flowRepository.save(flow);

            result.setSuccess(true);
            result.setEl(convertResult.getEl());
            result.setNodeCount(convertResult.getNodes().size());
            result.setMessage("部署成功");

            log.info("部署流程成功: flowId={}, el={}", flowId, convertResult.getEl());

        } catch (Exception e) {
            log.error("部署流程失败: flowId={}", flowId, e);

            // 更新失败状态
            flow.setDeployStatus(FlowDesign.DeployStatus.FAILED);
            flowRepository.save(flow);

            result.setSuccess(false);
            result.setErrorMessage("部署失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 卸载流程
     */
    public void undeployFlow(String flowId) {
        FlowDesign flow = getFlow(flowId);

        try {
            liteFlowDynamicService.removeChain(flowId);
        } catch (Exception e) {
            log.warn("卸载流程失败: flowId={}", flowId, e);
        }

        flow.setDeployStatus(FlowDesign.DeployStatus.NOT_DEPLOYED);
        flowRepository.save(flow);

        log.info("卸载流程成功: flowId={}", flowId);
    }

    /**
     * 重新部署所有已部署的流程
     */
    public int redeployAllFlows() {
        List<FlowDesign> deployedFlows = flowRepository.findByDeployStatus(FlowDesign.DeployStatus.DEPLOYED);
        List<FlowDesign> modifiedFlows = flowRepository.findByDeployStatus(FlowDesign.DeployStatus.MODIFIED);

        List<FlowDesign> toRedeploy = new ArrayList<>();
        toRedeploy.addAll(deployedFlows);
        toRedeploy.addAll(modifiedFlows);

        int successCount = 0;
        for (FlowDesign flow : toRedeploy) {
            try {
                DeployResult result = deployFlow(flow.getFlowId());
                if (result.isSuccess()) {
                    successCount++;
                }
            } catch (Exception e) {
                log.error("重新部署流程失败: flowId={}", flow.getFlowId(), e);
            }
        }

        log.info("重新部署流程完成: {}/{}", successCount, toRedeploy.size());
        return successCount;
    }

    // ==================== 流程执行 ====================

    /**
     * 执行流程
     */
    public ExecuteResult executeFlow(String flowId, Map<String, Object> params) {
        FlowDesign flow = getFlow(flowId);
        ExecuteResult result = new ExecuteResult();
        result.setFlowId(flowId);

        // 检查部署状态
        if (flow.getDeployStatus() != FlowDesign.DeployStatus.DEPLOYED) {
            // 自动部署
            DeployResult deployResult = deployFlow(flowId);
            if (!deployResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorMessage("流程未部署且自动部署失败: " + deployResult.getErrorMessage());
                return result;
            }
        }

        try {
            long startTime = System.currentTimeMillis();

            // 执行流程
            Map<String, Object> executeResult = liteFlowDynamicService.executeChain(flowId, params);

            result.setSuccess((Boolean) executeResult.getOrDefault("success", false));
            result.setContext((Map<String, Object>) executeResult.get("context"));
            result.setExecuteSteps((String) executeResult.get("executeSteps"));
            result.setExecutionTime(System.currentTimeMillis() - startTime);

            if (!result.isSuccess()) {
                result.setErrorMessage((String) executeResult.get("errorMessage"));
            }

            log.info("执行流程完成: flowId={}, success={}, time={}ms",
                    flowId, result.isSuccess(), result.getExecutionTime());

        } catch (Exception e) {
            log.error("执行流程失败: flowId={}", flowId, e);
            result.setSuccess(false);
            result.setErrorMessage("执行失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 直接执行流程设计 (不需要先保存和部署)
     */
    public ExecuteResult executeFlowDirect(FlowDesign flow, Map<String, Object> params) {
        ExecuteResult result = new ExecuteResult();
        result.setFlowId(flow.getFlowId());

        try {
            // 1. 验证流程
            FlowConvertService.ValidationResult validation = flowConvertService.validate(flow);
            if (!validation.isValid()) {
                result.setSuccess(false);
                result.setErrorMessage("流程验证失败: " + String.join(", ", validation.getErrors()));
                return result;
            }

            // 2. 转换流程
            ConvertResult convertResult = flowConvertService.convert(flow);
            if (!convertResult.isSuccess()) {
                result.setSuccess(false);
                result.setErrorMessage("转换失败: " + convertResult.getErrorMessage());
                return result;
            }

            // 3. 临时加载组件
            for (ConvertResult.NodeDefinition node : convertResult.getNodes()) {
                liteFlowDynamicService.addScriptNode(
                        node.getNodeId(),
                        node.getNodeName(),
                        node.getScript(),
                        node.getNodeType(),
                        node.getLanguage()
                );
            }

            // 4. 执行 EL 表达式
            long startTime = System.currentTimeMillis();
            Map<String, Object> executeResult = liteFlowDynamicService.executeEL(convertResult.getEl(), params);

            result.setSuccess((Boolean) executeResult.getOrDefault("success", false));
            result.setContext((Map<String, Object>) executeResult.get("context"));
            result.setExecuteSteps((String) executeResult.get("executeSteps"));
            result.setExecutionTime(System.currentTimeMillis() - startTime);

            if (!result.isSuccess()) {
                result.setErrorMessage((String) executeResult.get("errorMessage"));
            }

        } catch (Exception e) {
            log.error("直接执行流程失败: flowId={}", flow.getFlowId(), e);
            result.setSuccess(false);
            result.setErrorMessage("执行失败: " + e.getMessage());
        }

        return result;
    }

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类
     */
    public Set<String> getAllCategories() {
        return flowRepository.getAllCategories();
    }

    /**
     * 获取分类统计
     */
    public Map<String, Integer> getCategoryStats() {
        return flowRepository.countByCategory();
    }

    // ==================== 内部方法 ====================

    private void collectComponentIds(CmpProperty node, Set<String> componentIds) {
        if (node == null) return;

        if (node.isLeafNode()) {
            String nodeId = node.getNodeId();
            if (nodeId != null && !nodeId.isEmpty()) {
                componentIds.add(nodeId);
            }
        }

        if (node.getCondition() != null) {
            collectComponentIds(node.getCondition(), componentIds);
        }

        if (node.getChildren() != null) {
            for (CmpProperty child : node.getChildren()) {
                collectComponentIds(child, componentIds);
            }
        }
    }

    // ==================== 结果类 ====================

    /**
     * 部署结果
     */
    @lombok.Data
    public static class DeployResult {
        private String flowId;
        private boolean success;
        private String el;
        private int nodeCount;
        private String message;
        private String errorMessage;
    }

    /**
     * 执行结果
     */
    @lombok.Data
    public static class ExecuteResult {
        private String flowId;
        private boolean success;
        private Map<String, Object> context;
        private String executeSteps;
        private long executionTime;
        private String errorMessage;
    }
}

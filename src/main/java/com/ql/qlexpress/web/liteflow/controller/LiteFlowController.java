package com.ql.qlexpress.web.liteflow.controller;

import com.google.common.collect.Lists;
import com.ql.qlexpress.web.liteflow.dto.ChainRequest;
import com.ql.qlexpress.web.liteflow.dto.ExecuteELRequest;
import com.ql.qlexpress.web.liteflow.dto.FlowDefinitionRequest;
import com.ql.qlexpress.web.liteflow.dto.ScriptNodeRequest;
import com.ql.qlexpress.web.liteflow.service.LiteFlowDynamicService;
import com.ql.qlexpress.web.model.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LiteFlow + QLExpress 动态流程控制器
 * 使用通用 FlowContext (Map) 作为上下文
 */
@Slf4j
@RestController
@RequestMapping("/api/liteflow")
@RequiredArgsConstructor
public class LiteFlowController {

    private final LiteFlowDynamicService liteFlowDynamicService;

    // ==================== 动态加载和执行接口 ====================

    /**
     * 动态添加脚本节点
     *
     * @param request 脚本节点请求
     */
    @PostMapping("/dynamic/node")
    public ApiResponse<String> addScriptNode(@Validated @RequestBody ScriptNodeRequest request) {
        try {
            liteFlowDynamicService.addScriptNode(
                    request.getNodeId(),
                    request.getNodeName(),
                    request.getScript(),
                    request.getNodeType(),
                    request.getLanguage()
            );
            return ApiResponse.success("脚本节点添加成功: " + request.getNodeId());
        } catch (Exception e) {
            log.error("添加脚本节点失败: nodeId={}", request.getNodeId(), e);
            return ApiResponse.error("添加脚本节点失败: " + e.getMessage());
        }
    }

    /**
     * 动态刷新脚本节点
     *
     * @param nodeId 节点ID
     * @param script 新的脚本内容
     */
    @PutMapping("/dynamic/node/{nodeId}")
    public ApiResponse<String> reloadScriptNode(
            @PathVariable String nodeId,
            @RequestBody String script) {
        try {
            liteFlowDynamicService.reloadScriptNode(nodeId, script);
            return ApiResponse.success("脚本节点刷新成功: " + nodeId);
        } catch (Exception e) {
            log.error("刷新脚本节点失败: nodeId={}", nodeId, e);
            return ApiResponse.error("刷新脚本节点失败: " + e.getMessage());
        }
    }

    /**
     * 动态添加/更新流程链
     *
     * @param request 流程链请求
     */
    @PostMapping("/dynamic/chain")
    public ApiResponse<String> addOrUpdateChain(@Validated @RequestBody ChainRequest request) {
        try {
            liteFlowDynamicService.addOrUpdateChain(request.getChainName(), request.getEl());
            return ApiResponse.success("流程链添加/更新成功: " + request.getChainName());
        } catch (Exception e) {
            log.error("添加/更新流程链失败: chainName={}", request.getChainName(), e);
            return ApiResponse.error("添加/更新流程链失败: " + e.getMessage());
        }
    }

    /**
     * 刷新指定流程链
     *
     * @param chainId 流程链ID
     * @param el      新的EL表达式
     */
    @PutMapping("/dynamic/chain/{chainId}")
    public ApiResponse<String> reloadChain(
            @PathVariable String chainId,
            @RequestBody String el) {
        try {
            liteFlowDynamicService.reloadChain(chainId, el);
            return ApiResponse.success("流程链刷新成功: " + chainId);
        } catch (Exception e) {
            log.error("刷新流程链失败: chainId={}", chainId, e);
            return ApiResponse.error("刷新流程链失败: " + e.getMessage());
        }
    }

    /**
     * 移除流程链
     *
     * @param chainId 流程链ID
     */
    @DeleteMapping("/dynamic/chain/{chainId}")
    public ApiResponse<String> removeChain(@PathVariable String chainId) {
        try {
            liteFlowDynamicService.removeChain(chainId);
            return ApiResponse.success("流程链移除成功: " + chainId);
        } catch (Exception e) {
            log.error("移除流程链失败: chainId={}", chainId, e);
            return ApiResponse.error("移除流程链失败: " + e.getMessage());
        }
    }

    /**
     * 批量加载完整流程定义 (节点 + 链)
     *
     * @param request 流程定义请求
     */
    @PostMapping("/dynamic/load")
    public ApiResponse<String> loadFlowDefinition(@Validated @RequestBody FlowDefinitionRequest request) {
        try {
            List<LiteFlowDynamicService.NodeDefinition> nodes = request.getNodes() != null
                    ? request.getNodes().stream()
                    .map(n -> LiteFlowDynamicService.NodeDefinition.builder()
                            .nodeId(n.getNodeId())
                            .nodeName(n.getNodeName())
                            .script(n.getScript())
                            .nodeType(n.getNodeType())
                            .language(n.getLanguage())
                            .build())
                    .collect(Collectors.toList())
                    : Lists.newArrayList();

            List<LiteFlowDynamicService.ChainDefinition> chains = request.getChains() != null
                    ? request.getChains().stream()
                    .map(c -> LiteFlowDynamicService.ChainDefinition.builder()
                            .chainName(c.getChainName())
                            .el(c.getEl())
                            .build())
                    .collect(Collectors.toList())
                    : Lists.newArrayList();

            liteFlowDynamicService.loadFlowDefinition(nodes, chains);
            return ApiResponse.success("流程定义加载成功: " + nodes.size() + "个节点, " + chains.size() + "个链");
        } catch (Exception e) {
            log.error("批量加载流程定义失败", e);
            return ApiResponse.error("批量加载���程定义失败: " + e.getMessage());
        }
    }

    /**
     * 执行指定的流程链
     *
     * @param chainName 流程链名称
     * @param params    流程参数 (可选)
     */
    @PostMapping("/execute/{chainName}")
    public ApiResponse<Map<String, Object>> executeChain(
            @PathVariable String chainName,
            @RequestBody(required = false) Map<String, Object> params) {
        try {
            Map<String, Object> result = liteFlowDynamicService.executeChain(chainName, params);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("执行流程链失败: chainName={}", chainName, e);
            return ApiResponse.error("执行流程链失败: " + e.getMessage());
        }
    }

    /**
     * 直接执行EL表达式 (无需预先定义Chain)
     *
     * @param request 执行请求
     */
    @PostMapping("/dynamic/execute-el")
    public ApiResponse<Map<String, Object>> executeEL(@Validated @RequestBody ExecuteELRequest request) {
        try {
            Map<String, Object> result = liteFlowDynamicService.executeEL(request.getEl(), request.getParams());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("执行EL表达式失败: el={}", request.getEl(), e);
            return ApiResponse.error("执行EL表达式失败: " + e.getMessage());
        }
    }

    /**
     * 刷新所有规则
     */
    @PostMapping("/dynamic/reload-all")
    public ApiResponse<String> reloadAllChains() {
        try {
            liteFlowDynamicService.reloadAllChains();
            return ApiResponse.success("所有规则刷新成功");
        } catch (Exception e) {
            log.error("刷新所有规则失败", e);
            return ApiResponse.error("刷新所有规则失败: " + e.getMessage());
        }
    }
}

package com.ql.qlexpress.web.liteflow.service;

import com.ql.qlexpress.web.liteflow.context.FlowContext;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.meta.LiteflowMetaOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LiteFlow 动态脚本加载和执行服务
 * 支持动态加载脚本节点、构建流程链、执行流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiteFlowDynamicService {

    private final FlowExecutor flowExecutor;

    /**
     * 动态添加脚本节点
     *
     * @param nodeId   节点ID
     * @param nodeName 节点名称
     * @param script   脚本内容
     * @param nodeType 节点类型: script, boolean_script, switch_script
     * @param language 脚本语言: qlexpress, groovy, javascript 等
     */
    public void addScriptNode(String nodeId, String nodeName, String script, String nodeType, String language) {
        log.info("动态添加脚本节点: nodeId={}, nodeName={}, nodeType={}, language={}", nodeId, nodeName, nodeType, language);

        NodeTypeEnum nodeTypeEnum = parseNodeType(nodeType);

        switch (nodeTypeEnum) {
            case SCRIPT:
                LiteFlowNodeBuilder.createScriptNode()
                        .setId(nodeId)
                        .setName(nodeName)
                        .setScript(script)
                        .setLanguage(language)
                        .build();
                break;
            case BOOLEAN_SCRIPT:
                LiteFlowNodeBuilder.createScriptBooleanNode()
                        .setId(nodeId)
                        .setName(nodeName)
                        .setScript(script)
                        .setLanguage(language)
                        .build();
                break;
            case SWITCH_SCRIPT:
                LiteFlowNodeBuilder.createScriptSwitchNode()
                        .setId(nodeId)
                        .setName(nodeName)
                        .setScript(script)
                        .setLanguage(language)
                        .build();
                break;
            default:
                throw new IllegalArgumentException("不支持的脚本节点类型: " + nodeType);
        }

        log.info("脚本节点添加成功: nodeId={}", nodeId);
    }

    /**
     * 动态刷新/更新脚本节点
     *
     * @param nodeId 节点ID
     * @param script 新的脚本内容
     */
    public void reloadScriptNode(String nodeId, String script) {
        log.info("刷新脚本节点: nodeId={}", nodeId);
        LiteflowMetaOperator.reloadScript(nodeId, script);
        log.info("脚本节点刷新成功: nodeId={}", nodeId);
    }

    /**
     * 动态添加/更新流程链
     *
     * @param chainName 流程链名称
     * @param el        EL表达式
     */
    public void addOrUpdateChain(String chainName, String el) {
        log.info("动态添加/更新流程链: chainName={}, el={}", chainName, el);
        LiteFlowChainELBuilder.createChain()
                .setChainName(chainName)
                .setEL(el)
                .build();
        log.info("流程链添加/更新成功: chainName={}", chainName);
    }

    /**
     * 刷新指定流程链
     *
     * @param chainId 流程链ID
     * @param el      新的EL表达式
     */
    public void reloadChain(String chainId, String el) {
        log.info("刷新流程链: chainId={}, el={}", chainId, el);
        LiteflowMetaOperator.reloadOneChain(chainId, el);
        log.info("流程链刷新成功: chainId={}", chainId);
    }

    /**
     * 移除流程链
     *
     * @param chainId 流程链ID
     */
    public void removeChain(String chainId) {
        log.info("移除流程链: chainId={}", chainId);
        LiteflowMetaOperator.removeChain(chainId);
        log.info("流程链移除成功: chainId={}", chainId);
    }

    /**
     * 执行流程链
     *
     * @param chainName 流程链名称
     * @param params    流程参数
     * @return 执行结果
     */
    public Map<String, Object> executeChain(String chainName, Map<String, Object> params) {
        FlowContext context = new FlowContext();
        if (params != null) {
            params.forEach(context::setData);
        }

        log.info("开始执行流程: chainName={}, params={}", chainName, params);
        LiteflowResponse response = flowExecutor.execute2Resp(chainName, null, context);

        return buildExecutionResult(chainName, context, response);
    }

    /**
     * 直接执行EL表达式 (无需预先定义Chain)
     * 注意: LiteFlow会自动缓存相同MD5指纹的表达式，避免重复创建
     *
     * @param el     EL表达式
     * @param params 流程参数
     * @return 执行结果
     */
    public Map<String, Object> executeEL(String el, Map<String, Object> params) {
        // 为 EL 表达式生成一个临时的 chain 名称 (基于 MD5)
        String tempChainName = "temp_chain_" + Math.abs(el.hashCode());

        // 先动态创建 Chain
        addOrUpdateChain(tempChainName, el);

        // 然后执行
        return executeChain(tempChainName, params);
    }

    /**
     * 批量加载完整的流程定义 (节点 + 链)
     *
     * @param nodes  节点定义列表
     * @param chains 流程链定义列表
     */
    public void loadFlowDefinition(List<NodeDefinition> nodes, List<ChainDefinition> chains) {
        log.info("批量加载流程定义: nodes={}, chains={}", nodes.size(), chains.size());

        // 1. 先加载所有节点
        for (NodeDefinition node : nodes) {
            addScriptNode(node.getNodeId(), node.getNodeName(), node.getScript(), node.getNodeType(), node.getLanguage());
        }

        // 2. 再加载所有链 (因为链依赖节点)
        for (ChainDefinition chain : chains) {
            addOrUpdateChain(chain.getChainName(), chain.getEl());
        }

        log.info("流程定义加载完成");
    }

    /**
     * 刷新所有规则
     */
    public void reloadAllChains() {
        log.info("刷新所有规则");
        LiteflowMetaOperator.reloadAllChain();
        log.info("所有规则刷新完成");
    }

    private NodeTypeEnum parseNodeType(String nodeType) {
        if (nodeType == null || nodeType.isEmpty()) {
            return NodeTypeEnum.SCRIPT;
        }
        switch (nodeType.toLowerCase()) {
            case "script":
                return NodeTypeEnum.SCRIPT;
            case "boolean_script":
                return NodeTypeEnum.BOOLEAN_SCRIPT;
            case "switch_script":
                return NodeTypeEnum.SWITCH_SCRIPT;
            default:
                return NodeTypeEnum.SCRIPT;
        }
    }

    private Map<String, Object> buildExecutionResult(String chainName, FlowContext context, LiteflowResponse response) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", response.isSuccess());
        result.put("chainName", chainName);
        result.put("context", new HashMap<>(context));
        result.put("executeSteps", response.getExecuteStepStrWithTime());

        if (!response.isSuccess()) {
            result.put("errorMessage", response.getMessage());
            if (response.getCause() != null) {
                result.put("errorCause", response.getCause().getMessage());
            }
        }

        return result;
    }

    /**
     * 节点定义
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NodeDefinition {
        private String nodeId;
        private String nodeName;
        private String script;
        private String nodeType;  // script, boolean_script, switch_script
        private String language;  // qlexpress, groovy, javascript 等
    }

    /**
     * 流程链定义
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChainDefinition {
        private String chainName;
        private String el;
    }
}

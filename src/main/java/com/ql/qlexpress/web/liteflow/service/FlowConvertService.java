package com.ql.qlexpress.web.liteflow.service;

import com.ql.qlexpress.web.exception.BusinessException;
import com.ql.qlexpress.web.liteflow.model.CmpProperty;
import com.ql.qlexpress.web.liteflow.model.ConvertResult;
import com.ql.qlexpress.web.liteflow.model.FlowDesign;
import com.ql.qlexpress.web.liteflow.model.QLComponent;
import com.ql.qlexpress.web.liteflow.repository.QLComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程转换服务
 * 将可视化编排 JSON (CmpProperty) 转换为 LiteFlow EL 表达式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowConvertService {

    private final QLComponentRepository componentRepository;

    /**
     * 将流程设计转换为 LiteFlow 配置
     */
    public ConvertResult convert(FlowDesign flowDesign) {
        ConvertResult result = new ConvertResult();
        result.setFlowId(flowDesign.getFlowId());
        result.setChainName(flowDesign.getFlowId());

        try {
            // 1. 收集所有使用的组件ID
            Set<String> componentIds = new HashSet<>();
            collectComponentIds(flowDesign.getRoot(), componentIds);
            result.setUsedComponentIds(componentIds);

            // 2. 加载组件定义，生成节点列表
            List<ConvertResult.NodeDefinition> nodes = new ArrayList<>();
            for (String componentId : componentIds) {
                Optional<QLComponent> componentOpt = componentRepository.findById(componentId);
                if (componentOpt.isPresent()) {
                    QLComponent component = componentOpt.get();
                    nodes.add(ConvertResult.NodeDefinition.builder()
                            .nodeId(componentId)
                            .nodeName(component.getComponentName())
                            .nodeType(component.toLiteFlowNodeType())
                            .language(component.getLanguage())
                            .script(component.getScript())
                            .build());
                } else {
                    result.addWarning("组件不存在: " + componentId);
                }
            }
            result.setNodes(nodes);

            // 3. 生成 EL 表达式
            String el = generateEL(flowDesign.getRoot());
            result.setEl(el);

            result.setSuccess(true);
            log.info("流程转换成功: flowId={}, el={}", flowDesign.getFlowId(), el);

        } catch (Exception e) {
            log.error("流程转换失败: flowId={}", flowDesign.getFlowId(), e);
            result.setFailed("转换失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 仅生成 EL 表达式 (不加载组件脚本)
     */
    public String generateELOnly(CmpProperty root) {
        return generateEL(root);
    }

    /**
     * 验证流程定义
     */
    public ValidationResult validate(FlowDesign flowDesign) {
        ValidationResult result = new ValidationResult();
        result.setFlowId(flowDesign.getFlowId());

        try {
            // 1. 基础验证
            if (flowDesign.getRoot() == null) {
                result.addError("流程编排不能为空");
                return result;
            }

            // 2. 收集组件ID并验证
            Set<String> componentIds = new HashSet<>();
            collectComponentIds(flowDesign.getRoot(), componentIds);

            for (String componentId : componentIds) {
                if (!componentRepository.existsById(componentId)) {
                    result.addError("组件不存在: " + componentId);
                }
            }

            // 3. 结构验证
            validateStructure(flowDesign.getRoot(), result);

            // 4. 尝试生成 EL (语法验证)
            try {
                String el = generateEL(flowDesign.getRoot());
                result.setGeneratedEL(el);
            } catch (Exception e) {
                result.addError("EL 表达式生成失败: " + e.getMessage());
            }

            result.setValid(result.getErrors().isEmpty());

        } catch (Exception e) {
            result.addError("验证过程出错: " + e.getMessage());
            result.setValid(false);
        }

        return result;
    }

    /**
     * 递归收集所有组件ID
     */
    private void collectComponentIds(CmpProperty node, Set<String> componentIds) {
        if (node == null) return;

        // 如果是叶子节点，收集组件ID
        if (node.isLeafNode()) {
            String nodeId = node.getNodeId();
            if (nodeId != null && !nodeId.isEmpty()) {
                componentIds.add(nodeId);
            }
        }

        // 递归处理条件节点
        if (node.getCondition() != null) {
            collectComponentIds(node.getCondition(), componentIds);
        }

        // 递归处理子节点
        if (node.getChildren() != null) {
            for (CmpProperty child : node.getChildren()) {
                collectComponentIds(child, componentIds);
            }
        }
    }

    /**
     * 递归生成 EL 表达式
     */
    private String generateEL(CmpProperty node) {
        if (node == null) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "节点不能为空");
        }

        String type = node.getType();
        if (type == null || type.isEmpty()) {
            // 如果没有类型，视为组件引用
            return node.getNodeId();
        }

        switch (type.toUpperCase()) {
            case "THEN":
                return generateThen(node);
            case "WHEN":
                return generateWhen(node);
            case "IF":
                return generateIf(node);
            case "SWITCH":
                return generateSwitch(node);
            case "FOR":
                return generateFor(node);
            case "WHILE":
                return generateWhile(node);
            case "CATCH":
                return generateCatch(node);
            case "AND":
                return generateAnd(node);
            case "OR":
                return generateOr(node);
            case "NOT":
                return generateNot(node);
            case "COMMONNODE":
            case "BOOLEANNODE":
            case "SWITCHNODE":
            case "COMPONENT":
            default:
                // 叶子节点，返回组件ID
                return node.getNodeId();
        }
    }

    /**
     * 生成 THEN (顺序执行)
     */
    private String generateThen(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "THEN 节点必须有子节点");
        }

        String children = node.getChildren().stream()
                .map(this::generateEL)
                .collect(Collectors.joining(", "));

        String result = "THEN(" + children + ")";
        return appendModifiers(result, node.getProperties());
    }

    /**
     * 生成 WHEN (并行执行)
     */
    private String generateWhen(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "WHEN 节点必须有子节点");
        }

        String children = node.getChildren().stream()
                .map(this::generateEL)
                .collect(Collectors.joining(", "));

        String result = "WHEN(" + children + ")";

        // 处理 WHEN 特有的修饰符
        if (node.getProperties() != null) {
            CmpProperty.Properties props = node.getProperties();
            if (props.getMaxWaitSeconds() != null) {
                result += ".maxWaitSeconds(" + props.getMaxWaitSeconds() + ")";
            }
            if (Boolean.TRUE.equals(props.getIgnoreError())) {
                result += ".ignoreError(true)";
            }
            if (Boolean.TRUE.equals(props.getMust())) {
                result += ".must()";
            }
        }

        return appendModifiers(result, node.getProperties());
    }

    /**
     * 生成 IF (条件分支)
     */
    private String generateIf(CmpProperty node) {
        if (node.getCondition() == null) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "IF 节点必须有条件");
        }

        String condition = generateEL(node.getCondition());
        List<CmpProperty> children = node.getChildren();

        if (children == null || children.isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "IF 节点必须有分支");
        }

        StringBuilder result = new StringBuilder();
        result.append("IF(").append(condition).append(", ");

        // true 分支
        result.append(generateEL(children.get(0)));

        // false 分支 (可选)
        if (children.size() > 1) {
            result.append(", ").append(generateEL(children.get(1)));
        }

        result.append(")");

        return appendModifiers(result.toString(), node.getProperties());
    }

    /**
     * 生成 SWITCH (多路分支)
     */
    private String generateSwitch(CmpProperty node) {
        if (node.getCondition() == null) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "SWITCH 节点必须有条件");
        }

        String condition = generateEL(node.getCondition());
        List<CmpProperty> children = node.getChildren();

        if (children == null || children.isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "SWITCH 节点必须有分支");
        }

        StringBuilder result = new StringBuilder();
        result.append("SWITCH(").append(condition).append(").to(");

        // 收集分支节点和分支ID
        List<String> branchNodes = new ArrayList<>();
        List<String> branchIds = new ArrayList<>();

        for (CmpProperty child : children) {
            branchNodes.add(generateEL(child));

            // 获取分支ID
            String branchId = null;
            if (child.getProperties() != null && child.getProperties().getBranch() != null) {
                branchId = child.getProperties().getBranch();
            } else {
                branchId = child.getNodeId();
            }
            branchIds.add("'" + branchId + "'");
        }

        result.append(String.join(", ", branchNodes));
        result.append(")");

        // 如果有分支ID映射，添加 .id()
        if (!branchIds.isEmpty()) {
            result.append(".id(").append(String.join(", ", branchIds)).append(")");
        }

        return appendModifiers(result.toString(), node.getProperties());
    }

    /**
     * 生成 FOR (循环)
     */
    private String generateFor(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "FOR 节点必须有子节点");
        }

        Integer loopCount = null;
        if (node.getProperties() != null) {
            loopCount = node.getProperties().getLoopCount();
        }

        if (loopCount == null) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "FOR 节点必须指定循环次数");
        }

        String body = generateEL(node.getChildren().get(0));
        String result = "FOR(" + loopCount + ").DO(" + body + ")";

        // 处理 BREAK 条件
        if (node.getProperties() != null && node.getProperties().getBreakCondition() != null) {
            String breakCond = generateEL(node.getProperties().getBreakCondition());
            result += ".BREAK(" + breakCond + ")";
        }

        return appendModifiers(result, node.getProperties());
    }

    /**
     * 生成 WHILE (条件循环)
     */
    private String generateWhile(CmpProperty node) {
        if (node.getCondition() == null) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "WHILE 节点必须有条件");
        }
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "WHILE 节点必须有子节点");
        }

        String condition = generateEL(node.getCondition());
        String body = generateEL(node.getChildren().get(0));

        String result = "WHILE(" + condition + ").DO(" + body + ")";

        // 处理 BREAK 条件
        if (node.getProperties() != null && node.getProperties().getBreakCondition() != null) {
            String breakCond = generateEL(node.getProperties().getBreakCondition());
            result += ".BREAK(" + breakCond + ")";
        }

        return appendModifiers(result, node.getProperties());
    }

    /**
     * 生成 CATCH (异常捕获)
     */
    private String generateCatch(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "CATCH 节点必须有子节点");
        }

        String tryBody = generateEL(node.getChildren().get(0));
        StringBuilder result = new StringBuilder();
        result.append("CATCH(").append(tryBody).append(")");

        // 处理 catch 分支
        if (node.getChildren().size() > 1) {
            String catchBody = generateEL(node.getChildren().get(1));
            result.append(".DO(").append(catchBody).append(")");
        }

        return appendModifiers(result.toString(), node.getProperties());
    }

    /**
     * 生成 AND (逻辑与)
     */
    private String generateAnd(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().size() < 2) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "AND 节点至少需要两个子节点");
        }

        String children = node.getChildren().stream()
                .map(this::generateEL)
                .collect(Collectors.joining(", "));

        return "AND(" + children + ")";
    }

    /**
     * 生成 OR (逻辑或)
     */
    private String generateOr(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().size() < 2) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "OR 节点至少需要两个子节点");
        }

        String children = node.getChildren().stream()
                .map(this::generateEL)
                .collect(Collectors.joining(", "));

        return "OR(" + children + ")";
    }

    /**
     * 生成 NOT (逻辑非)
     */
    private String generateNot(CmpProperty node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            throw new BusinessException("FLOW_CONVERT_ERROR", "NOT 节点必须有子节点");
        }

        String child = generateEL(node.getChildren().get(0));
        return "NOT(" + child + ")";
    }

    /**
     * 添加通用修饰符 (.id(), .tag(), .data())
     */
    private String appendModifiers(String el, CmpProperty.Properties properties) {
        if (properties == null) {
            return el;
        }

        StringBuilder result = new StringBuilder(el);

        if (properties.getId() != null && !properties.getId().isEmpty()) {
            result.append(".id(\"").append(properties.getId()).append("\")");
        }

        if (properties.getTag() != null && !properties.getTag().isEmpty()) {
            result.append(".tag(\"").append(properties.getTag()).append("\")");
        }

        if (properties.getData() != null && !properties.getData().isEmpty()) {
            result.append(".data(\"").append(properties.getData()).append("\")");
        }

        return result.toString();
    }

    /**
     * 结构验证
     */
    private void validateStructure(CmpProperty node, ValidationResult result) {
        if (node == null) return;

        String type = node.getType();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "THEN":
                case "WHEN":
                    if (node.getChildren() == null || node.getChildren().isEmpty()) {
                        result.addWarning(type + " 节点没有子节点");
                    }
                    break;
                case "IF":
                    if (node.getCondition() == null) {
                        result.addError("IF 节点缺少条件");
                    }
                    if (node.getChildren() == null || node.getChildren().isEmpty()) {
                        result.addError("IF 节点缺少分支");
                    }
                    break;
                case "SWITCH":
                    if (node.getCondition() == null) {
                        result.addError("SWITCH 节点缺少条件");
                    }
                    if (node.getChildren() == null || node.getChildren().isEmpty()) {
                        result.addError("SWITCH 节点缺少分支");
                    }
                    break;
            }
        }

        // 递归验证
        if (node.getCondition() != null) {
            validateStructure(node.getCondition(), result);
        }
        if (node.getChildren() != null) {
            for (CmpProperty child : node.getChildren()) {
                validateStructure(child, result);
            }
        }
    }

    /**
     * 验证结果
     */
    @lombok.Data
    public static class ValidationResult {
        private String flowId;
        private boolean valid = true;
        private String generatedEL;
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
}

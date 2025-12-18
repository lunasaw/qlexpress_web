package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.transpiler.FlowGraph;
import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.model.enums.FlowErrorType;
import com.ql.qlexpress.web.model.enums.ValidationLevel;
import com.ql.qlexpress.web.model.visual.*;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.exception.QLSyntaxException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 验证服务
 *
 * @author qlexpress
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateService {

    private static final Pattern ERROR_LOCATION_PATTERN = Pattern.compile("line (\\d+)[,:]?\\s*(?:column )?(\\d+)?");

    /**
     * 验证QL脚本
     */
    public ValidateScriptResponse validateScript(ValidateScriptRequest request) {
        log.info("验证QL脚本: {} 字符", request.getScript().length());

        List<SyntaxError> syntaxErrors = new ArrayList<>();
        List<SemanticWarning> semanticWarnings = new ArrayList<>();
        List<BestPracticeSuggestion> suggestions = new ArrayList<>();

        try {
            // 使用QLExpress进行语法检查
            Express4Runner runner = new Express4Runner(InitOptions.builder().build());
            runner.check(request.getScript());

            // 如果编译通过，检查语义
            if (request.isCheckSemantics()) {
                semanticWarnings.addAll(checkSemantics(request.getScript()));
            }

            // 如果需要检查最佳实践
            if (request.isCheckBestPractices()) {
                suggestions.addAll(checkBestPractices(request.getScript()));
            }

            // 计算复杂度
            ComplexityAnalysis complexity = analyzeComplexity(request.getScript());

            return ValidateScriptResponse.builder()
                    .valid(true)
                    .semanticWarnings(semanticWarnings)
                    .suggestions(suggestions)
                    .complexity(complexity)
                    .build();

        } catch (QLSyntaxException e) {
            log.warn("脚本语法错误: {}", e.getMessage());
            syntaxErrors.add(createSyntaxError(e));

            return ValidateScriptResponse.builder()
                    .valid(false)
                    .syntaxErrors(syntaxErrors)
                    .build();

        } catch (Exception e) {
            log.error("脚本验证异常: {}", e.getMessage(), e);
            syntaxErrors.add(SyntaxError.builder()
                    .line(1)
                    .column(1)
                    .message(e.getMessage())
                    .code("UNKNOWN_ERROR")
                    .build());

            return ValidateScriptResponse.builder()
                    .valid(false)
                    .syntaxErrors(syntaxErrors)
                    .build();
        }
    }

    /**
     * 验证可视化流程
     */
    public ValidateFlowResponse validateFlow(ValidateFlowRequest request) {
        log.info("验证可视化流程，验证级别: {}", request.getLevel());

        VisualFlowSchema flow = request.getFlow();
        ValidateFlowResponse response = ValidateFlowResponse.builder().build();

        // 基础验证
        validateStructure(flow, response);

        // 如果有结构错误，直接返回
        if (!response.getStructureErrors().isEmpty()) {
            response.setValid(false);
            return response;
        }

        // 节点验证
        validateNodes(flow, response);

        // 边验证
        validateEdges(flow, response);

        // 如果是严格模式，进行类型检查
        if (request.getLevel() == ValidationLevel.STRICT) {
            validateTypes(flow, response);
        }

        // 检查警告
        checkWarnings(flow, response);

        // 设置验证结果
        response.setValid(!response.hasErrors());

        return response;
    }

    /**
     * 验证流程结构
     */
    private void validateStructure(VisualFlowSchema flow, ValidateFlowResponse response) {
        List<VisualNode> nodes = flow.getNodes();
        List<VisualEdge> edges = flow.getEdges();

        // 检查空流程
        if (nodes == null || nodes.isEmpty()) {
            response.getStructureErrors().add(
                    FlowError.of(FlowErrorType.EMPTY_FLOW, "流程不能为空"));
            return;
        }

        // 检查起始节点
        long startCount = nodes.stream()
                .filter(n -> "start".equals(n.getType()))
                .count();

        if (startCount == 0) {
            response.getStructureErrors().add(
                    FlowError.of(FlowErrorType.NO_START_NODE, "流程缺少起始节点"));
        } else if (startCount > 1) {
            response.getStructureErrors().add(
                    FlowError.of(FlowErrorType.MULTIPLE_START_NODES, "流程存在多个起始节点"));
        }

        // 检查结束节点
        long endCount = nodes.stream()
                .filter(n -> "end".equals(n.getType()))
                .count();

        if (endCount == 0) {
            response.getStructureErrors().add(
                    FlowError.of(FlowErrorType.NO_END_NODE, "流程缺少结束节点"));
        }

        // 检查节点ID重复
        Set<String> nodeIds = new HashSet<>();
        for (VisualNode node : nodes) {
            if (!nodeIds.add(node.getId())) {
                response.getStructureErrors().add(
                        FlowError.of(FlowErrorType.DUPLICATE_NODE_ID,
                                "节点ID重复: " + node.getId()));
            }
        }

        // 检查边ID重复
        if (edges != null) {
            Set<String> edgeIds = new HashSet<>();
            for (VisualEdge edge : edges) {
                if (!edgeIds.add(edge.getId())) {
                    response.getStructureErrors().add(
                            FlowError.of(FlowErrorType.DUPLICATE_EDGE_ID,
                                    "边ID重复: " + edge.getId()));
                }
            }
        }

        // 检查不可达节点
        if (!response.getStructureErrors().isEmpty()) {
            return;
        }

        FlowGraph graph = FlowGraph.build(nodes, edges != null ? edges : Collections.emptyList());
        Set<String> reachable = graph.findReachableNodes();

        for (VisualNode node : nodes) {
            if (!reachable.contains(node.getId()) && !"start".equals(node.getType())) {
                response.getStructureErrors().add(
                        FlowError.of(FlowErrorType.UNREACHABLE_NODES,
                                "存在不可达节点: " + node.getId()));
            }
        }

        // 检查是否有循环（注意：某些循环是合法的，如循环结构）
        // 这里只检查是否存在非法循环
    }

    /**
     * 验证节点
     */
    private void validateNodes(VisualFlowSchema flow, ValidateFlowResponse response) {
        for (VisualNode node : flow.getNodes()) {
            validateNode(node, response);
        }
    }

    /**
     * 验证单个节点
     */
    private void validateNode(VisualNode node, ValidateFlowResponse response) {
        if (node.getId() == null || node.getId().isEmpty()) {
            response.getNodeErrors().add(
                    NodeError.of(null, node.getType(), "MISSING_ID", "节点缺少ID"));
            return;
        }

        if (node.getType() == null || node.getType().isEmpty()) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), null, "MISSING_TYPE", "节点缺少类型"));
            return;
        }

        // 根据节点类型验证数据
        NodeData data = node.getData();
        switch (node.getType()) {
            case "if":
                validateIfNode(node, data, response);
                break;
            case "for":
                validateForNode(node, data, response);
                break;
            case "while":
                validateWhileNode(node, data, response);
                break;
            case "expression":
                validateExpressionNode(node, data, response);
                break;
            case "assignment":
                validateAssignmentNode(node, data, response);
                break;
            case "function_call":
                validateFunctionCallNode(node, data, response);
                break;
            case "return":
                // return节点可以没有值
                break;
            case "start":
            case "end":
                // 起始和结束节点不需要额外验证
                break;
            default:
                response.getWarnings().add(
                        FlowWarning.forNode(node.getId(), "UNKNOWN_TYPE",
                                "未知的节点类型: " + node.getType()));
        }
    }

    private void validateIfNode(VisualNode node, NodeData data, ValidateFlowResponse response) {
        if (!(data instanceof IfNodeData)) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), "if", "INVALID_DATA", "if节点数据类型不正确"));
            return;
        }
        IfNodeData ifData = (IfNodeData) data;
        if (ifData.getCondition() == null) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "if", "condition", "if节点缺少条件表达式"));
        }
    }

    private void validateForNode(VisualNode node, NodeData data, ValidateFlowResponse response) {
        if (!(data instanceof ForNodeData)) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), "for", "INVALID_DATA", "for节点数据类型不正确"));
            return;
        }
        ForNodeData forData = (ForNodeData) data;
        if (forData.getCondition() == null) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "for", "condition", "for节点缺少条件表达式"));
        }
    }

    private void validateWhileNode(VisualNode node, NodeData data, ValidateFlowResponse response) {
        if (!(data instanceof WhileNodeData)) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), "while", "INVALID_DATA", "while节点数据类型不正确"));
            return;
        }
        WhileNodeData whileData = (WhileNodeData) data;
        if (whileData.getCondition() == null) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "while", "condition", "while节点缺少条件表达式"));
        }
    }

    private void validateExpressionNode(VisualNode node, NodeData data, ValidateFlowResponse response) {
        if (!(data instanceof ExpressionNodeData)) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), "expression", "INVALID_DATA", "expression节点数据类型不正确"));
            return;
        }
        ExpressionNodeData exprData = (ExpressionNodeData) data;
        if (exprData.getExpression() == null) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "expression", "expression", "expression节点缺少表达式"));
        }
    }

    private void validateAssignmentNode(VisualNode node, NodeData data, ValidateFlowResponse response) {
        if (!(data instanceof AssignmentNodeData)) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), "assignment", "INVALID_DATA", "assignment节点数据类型不正确"));
            return;
        }
        AssignmentNodeData assignData = (AssignmentNodeData) data;
        if (assignData.getVariable() == null || assignData.getVariable().isEmpty()) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "assignment", "variable", "assignment节点缺少变量名"));
        }
        if (assignData.getValue() == null) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "assignment", "value", "assignment节点缺少赋值表达式"));
        }
    }

    private void validateFunctionCallNode(VisualNode node, NodeData data, ValidateFlowResponse response) {
        if (!(data instanceof FunctionCallNodeData)) {
            response.getNodeErrors().add(
                    NodeError.of(node.getId(), "function_call", "INVALID_DATA", "function_call节点数据类型不正确"));
            return;
        }
        FunctionCallNodeData funcData = (FunctionCallNodeData) data;
        if (funcData.getFunctionName() == null || funcData.getFunctionName().isEmpty()) {
            response.getNodeErrors().add(
                    NodeError.propertyError(node.getId(), "function_call", "functionName", "function_call节点缺少函数名"));
        }
    }

    /**
     * 验证边
     */
    private void validateEdges(VisualFlowSchema flow, ValidateFlowResponse response) {
        if (flow.getEdges() == null) {
            return;
        }

        Set<String> nodeIds = new HashSet<>();
        for (VisualNode node : flow.getNodes()) {
            nodeIds.add(node.getId());
        }

        for (VisualEdge edge : flow.getEdges()) {
            if (!nodeIds.contains(edge.getSource())) {
                response.getEdgeErrors().add(
                        EdgeError.of(edge.getId(), edge.getSource(), edge.getTarget(),
                                "INVALID_SOURCE", "边的源节点不存在: " + edge.getSource()));
            }
            if (!nodeIds.contains(edge.getTarget())) {
                response.getEdgeErrors().add(
                        EdgeError.of(edge.getId(), edge.getSource(), edge.getTarget(),
                                "INVALID_TARGET", "边的目标节点不存在: " + edge.getTarget()));
            }
        }
    }

    /**
     * 验证类型
     */
    private void validateTypes(VisualFlowSchema flow, ValidateFlowResponse response) {
        // 类型检查的简化实现
        // TODO: 实现完整的类型推断和检查
    }

    /**
     * 检查警告
     */
    private void checkWarnings(VisualFlowSchema flow, ValidateFlowResponse response) {
        // 检查是否有未使用的变量
        // 检查是否有复杂度过高的表达式
        // 等等
    }

    /**
     * 创建语法错误
     */
    private SyntaxError createSyntaxError(QLSyntaxException e) {
        int line = 1;
        int column = 1;

        // 尝试从错误消息中提取位置信息
        String message = e.getMessage();
        if (message != null) {
            Matcher matcher = ERROR_LOCATION_PATTERN.matcher(message);
            if (matcher.find()) {
                line = Integer.parseInt(matcher.group(1));
                if (matcher.group(2) != null) {
                    column = Integer.parseInt(matcher.group(2));
                }
            }
        }

        return SyntaxError.builder()
                .line(line)
                .column(column)
                .endLine(line)
                .endColumn(column)
                .message(message)
                .code("SYNTAX_ERROR")
                .build();
    }

    /**
     * 检查语义
     */
    private List<SemanticWarning> checkSemantics(String script) {
        List<SemanticWarning> warnings = new ArrayList<>();

        // 检查可能的空指针访问
        if (script.contains("null.")) {
            warnings.add(SemanticWarning.builder()
                    .code("POSSIBLE_NPE")
                    .message("可能存在空指针访问")
                    .severity(SemanticWarning.WarningSeverity.WARNING)
                    .build());
        }

        // 检查除零风险
        if (script.contains("/ 0") || script.contains("/0")) {
            warnings.add(SemanticWarning.builder()
                    .code("DIVISION_BY_ZERO")
                    .message("可能存在除零错误")
                    .severity(SemanticWarning.WarningSeverity.WARNING)
                    .build());
        }

        return warnings;
    }

    /**
     * 检查最佳实践
     */
    private List<BestPracticeSuggestion> checkBestPractices(String script) {
        List<BestPracticeSuggestion> suggestions = new ArrayList<>();

        // 检查是否使用了魔法数字
        Pattern magicNumber = Pattern.compile("\\b\\d{3,}\\b");
        if (magicNumber.matcher(script).find()) {
            suggestions.add(BestPracticeSuggestion.builder()
                    .type("MAGIC_NUMBER")
                    .message("建议将魔法数字定义为常量")
                    .suggestion("使用有意义的常量名替代直接使用的数字")
                    .build());
        }

        // 检查嵌套深度
        int maxDepth = calculateMaxNestingDepth(script);
        if (maxDepth > 4) {
            suggestions.add(BestPracticeSuggestion.builder()
                    .type("DEEP_NESTING")
                    .message("代码嵌套层级过深: " + maxDepth)
                    .suggestion("考虑重构代码，减少嵌套层级")
                    .build());
        }

        return suggestions;
    }

    /**
     * 分析复杂度
     */
    private ComplexityAnalysis analyzeComplexity(String script) {
        String[] lines = script.split("\n");
        int lineCount = lines.length;

        // 计算圈复杂度（简化版：统计分支和循环语句）
        int cyclomaticComplexity = 1; // 基础复杂度
        int conditionCount = 0;
        int loopCount = 0;
        int functionCallCount = 0;

        Pattern ifPattern = Pattern.compile("\\bif\\s*\\(");
        Pattern forPattern = Pattern.compile("\\bfor\\s*\\(");
        Pattern whilePattern = Pattern.compile("\\bwhile\\s*\\(");
        Pattern funcPattern = Pattern.compile("\\w+\\s*\\(");

        for (String line : lines) {
            Matcher ifMatcher = ifPattern.matcher(line);
            while (ifMatcher.find()) {
                cyclomaticComplexity++;
                conditionCount++;
            }

            Matcher forMatcher = forPattern.matcher(line);
            while (forMatcher.find()) {
                cyclomaticComplexity++;
                loopCount++;
            }

            Matcher whileMatcher = whilePattern.matcher(line);
            while (whileMatcher.find()) {
                cyclomaticComplexity++;
                loopCount++;
            }

            // 统计函数调用（排除关键字）
            Matcher funcMatcher = funcPattern.matcher(line);
            while (funcMatcher.find()) {
                String match = funcMatcher.group();
                if (!match.startsWith("if") && !match.startsWith("for")
                        && !match.startsWith("while") && !match.startsWith("function")) {
                    functionCallCount++;
                }
            }
        }

        int maxNestingDepth = calculateMaxNestingDepth(script);

        // 统计变量数量（简��版：统计赋值语句）
        int variableCount = 0;
        Pattern varPattern = Pattern.compile("\\b(\\w+)\\s*=\\s*[^=]");
        Set<String> variables = new HashSet<>();
        Matcher varMatcher = varPattern.matcher(script);
        while (varMatcher.find()) {
            variables.add(varMatcher.group(1));
        }
        variableCount = variables.size();

        return ComplexityAnalysis.builder()
                .cyclomaticComplexity(cyclomaticComplexity)
                .maxNestingDepth(maxNestingDepth)
                .lineCount(lineCount)
                .variableCount(variableCount)
                .functionCallCount(functionCallCount)
                .loopCount(loopCount)
                .conditionCount(conditionCount)
                .level(ComplexityAnalysis.calculateLevel(cyclomaticComplexity))
                .build();
    }

    /**
     * 计算最大嵌套深度
     */
    private int calculateMaxNestingDepth(String script) {
        int maxDepth = 0;
        int currentDepth = 0;

        for (char c : script.toCharArray()) {
            if (c == '{') {
                currentDepth++;
                maxDepth = Math.max(maxDepth, currentDepth);
            } else if (c == '}') {
                currentDepth--;
            }
        }

        return maxDepth;
    }
}

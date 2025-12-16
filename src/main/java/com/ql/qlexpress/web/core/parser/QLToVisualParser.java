package com.ql.qlexpress.web.core.parser;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.aparser.QLParser;
import com.alibaba.qlexpress4.exception.QLSyntaxException;
import com.ql.qlexpress.web.model.visual.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * QL脚本到可视化流程的解析器
 *
 * 将QL脚本文本解析为可视化流程图JSON结构
 *
 * @author qlexpress
 */
@Slf4j
@Component
public class QLToVisualParser {

    private final Express4Runner runner;

    /**
     * 节点ID计数器
     */
    private int nodeIdCounter;

    /**
     * 边ID计数器
     */
    private int edgeIdCounter;

    /**
     * 节点Y坐标偏移
     */
    private static final int NODE_Y_OFFSET = 100;

    /**
     * 节点X坐标
     */
    private static final int NODE_X_BASE = 250;

    public QLToVisualParser() {
        this.runner = new Express4Runner(InitOptions.builder().build());
    }

    /**
     * 解析QL脚本为可视化流程
     *
     * @param script QL脚本
     * @return 可视化流程定义
     */
    public ParseResult parse(String script) {
        log.info("开始解析QL脚本，长度: {} 字符", script.length());

        // 重置计数器
        nodeIdCounter = 0;
        edgeIdCounter = 0;

        try {
            // 首先验证脚本语法
            runner.check(script);

            // 解析语法树
            QLParser.ProgramContext programContext = runner.parseToSyntaxTree(script);

            // 创建节点和边列表
            List<VisualNode> nodes = new ArrayList<>();
            List<VisualEdge> edges = new ArrayList<>();

            // 添加起始节点
            VisualNode startNode = createStartNode();
            nodes.add(startNode);

            // 解析脚本语句
            String prevNodeId = startNode.getId();
            int yPos = NODE_Y_OFFSET;

            // 简化解析：按行分割脚本并解析每个语句
            String[] lines = script.split("\n");
            StringBuilder currentStatement = new StringBuilder();
            int braceCount = 0;

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("//")) {
                    continue;
                }

                currentStatement.append(line).append("\n");

                // 计算大括号数量
                for (char c : trimmedLine.toCharArray()) {
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                }

                // 当语句完整时（大括号平衡且以分号或大括号结束）
                if (braceCount == 0 && (trimmedLine.endsWith(";") || trimmedLine.endsWith("}"))) {
                    String statement = currentStatement.toString().trim();
                    currentStatement = new StringBuilder();

                    if (!statement.isEmpty()) {
                        yPos += NODE_Y_OFFSET;
                        StatementParseResult result = parseStatement(statement, yPos);

                        if (result != null && !result.nodes.isEmpty()) {
                            nodes.addAll(result.nodes);
                            edges.addAll(result.edges);

                            // 连接前一个节点到当前语句的第一个节点
                            String firstNodeId = result.nodes.get(0).getId();
                            edges.add(createEdge(prevNodeId, firstNodeId));

                            // 更新前一个节点ID
                            prevNodeId = result.lastNodeId;
                            yPos = result.lastY;
                        }
                    }
                }
            }

            // 添加结束节点
            yPos += NODE_Y_OFFSET;
            VisualNode endNode = createEndNode(yPos);
            nodes.add(endNode);
            edges.add(createEdge(prevNodeId, endNode.getId()));

            // 构建流程定义
            VisualFlowSchema flow = new VisualFlowSchema();
            flow.setVersion("1.0");
            flow.setNodes(nodes);
            flow.setEdges(edges);

            FlowMetadata metadata = new FlowMetadata();
            metadata.setName("解析的流程");
            metadata.setDescription("从QL脚本解析生成");
            flow.setMetadata(metadata);

            log.info("QL脚本解析成功，生成节点: {}个，边: {}条", nodes.size(), edges.size());

            return ParseResult.success(flow);

        } catch (QLSyntaxException e) {
            log.error("QL脚本语法错误: {}", e.getMessage());
            return ParseResult.failure("语法错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("QL脚本解析异常: {}", e.getMessage(), e);
            return ParseResult.failure("解析异常: " + e.getMessage());
        }
    }

    /**
     * 解析单个语句
     */
    private StatementParseResult parseStatement(String statement, int yPos) {
        String trimmed = statement.trim();

        // if语句
        if (trimmed.startsWith("if")) {
            return parseIfStatement(trimmed, yPos);
        }

        // for语句
        if (trimmed.startsWith("for")) {
            return parseForStatement(trimmed, yPos);
        }

        // while语句
        if (trimmed.startsWith("while")) {
            return parseWhileStatement(trimmed, yPos);
        }

        // return语句
        if (trimmed.startsWith("return")) {
            return parseReturnStatement(trimmed, yPos);
        }

        // 赋值语句
        if (trimmed.contains("=") && !trimmed.contains("==") && !trimmed.contains("!=")
                && !trimmed.contains(">=") && !trimmed.contains("<=")) {
            return parseAssignmentStatement(trimmed, yPos);
        }

        // 函数调用
        if (trimmed.matches("\\w+\\s*\\(.*\\)\\s*;?")) {
            return parseFunctionCallStatement(trimmed, yPos);
        }

        // 默认作为表达式处理
        return parseExpressionStatement(trimmed, yPos);
    }

    /**
     * 解析if语句
     */
    private StatementParseResult parseIfStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 提取条件
        int condStart = statement.indexOf('(');
        int condEnd = findMatchingParen(statement, condStart);
        String condition = statement.substring(condStart + 1, condEnd).trim();

        // 创建if节点
        String nodeId = generateNodeId();
        VisualNode ifNode = new VisualNode();
        ifNode.setId(nodeId);
        ifNode.setType("if");
        ifNode.setPosition(new Position(NODE_X_BASE, yPos));

        IfNodeData data = new IfNodeData();
        data.setLabel("if (" + condition + ")");
        data.setCondition(Expression.variable(condition));
        ifNode.setData(data);

        nodes.add(ifNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 解析for语句
     */
    private StatementParseResult parseForStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 提取for循环部分
        int parenStart = statement.indexOf('(');
        int parenEnd = findMatchingParen(statement, parenStart);
        String forContent = statement.substring(parenStart + 1, parenEnd);

        String[] parts = forContent.split(";");
        String init = parts.length > 0 ? parts[0].trim() : "";
        String condition = parts.length > 1 ? parts[1].trim() : "";
        String update = parts.length > 2 ? parts[2].trim() : "";

        // 创建for节点
        String nodeId = generateNodeId();
        VisualNode forNode = new VisualNode();
        forNode.setId(nodeId);
        forNode.setType("for");
        forNode.setPosition(new Position(NODE_X_BASE, yPos));

        ForNodeData data = new ForNodeData();
        data.setLabel("for (" + init + "; " + condition + "; " + update + ")");
        data.setInit(Expression.variable(init));
        data.setCondition(Expression.variable(condition));
        data.setUpdate(Expression.variable(update));
        forNode.setData(data);

        nodes.add(forNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 解析while语句
     */
    private StatementParseResult parseWhileStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 提取条件
        int condStart = statement.indexOf('(');
        int condEnd = findMatchingParen(statement, condStart);
        String condition = statement.substring(condStart + 1, condEnd).trim();

        // 创建while节点
        String nodeId = generateNodeId();
        VisualNode whileNode = new VisualNode();
        whileNode.setId(nodeId);
        whileNode.setType("while");
        whileNode.setPosition(new Position(NODE_X_BASE, yPos));

        WhileNodeData data = new WhileNodeData();
        data.setLabel("while (" + condition + ")");
        data.setCondition(Expression.variable(condition));
        whileNode.setData(data);

        nodes.add(whileNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 解析return语句
     */
    private StatementParseResult parseReturnStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 提取返回值
        String returnValue = statement.replaceFirst("return\\s*", "")
                .replace(";", "").trim();

        String nodeId = generateNodeId();
        VisualNode returnNode = new VisualNode();
        returnNode.setId(nodeId);
        returnNode.setType("return");
        returnNode.setPosition(new Position(NODE_X_BASE, yPos));

        ReturnNodeData data = new ReturnNodeData();
        data.setLabel("return " + returnValue);
        data.setValue(returnValue.isEmpty() ? null : Expression.variable(returnValue));
        returnNode.setData(data);

        nodes.add(returnNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 解析赋值语句
     */
    private StatementParseResult parseAssignmentStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 分割变量和值
        int eqIndex = statement.indexOf('=');
        String varPart = statement.substring(0, eqIndex).trim();
        String valuePart = statement.substring(eqIndex + 1).replace(";", "").trim();

        // 检查是否有类型声明
        String varName = varPart;
        String varType = null;
        String[] varParts = varPart.split("\\s+");
        if (varParts.length > 1) {
            varType = varParts[0];
            varName = varParts[varParts.length - 1];
        }

        String nodeId = generateNodeId();
        VisualNode assignNode = new VisualNode();
        assignNode.setId(nodeId);
        assignNode.setType("assignment");
        assignNode.setPosition(new Position(NODE_X_BASE, yPos));

        AssignmentNodeData data = new AssignmentNodeData();
        String label = varType != null ? varType + " " + varName + " = " + valuePart : varName + " = " + valuePart;
        data.setLabel(label);
        data.setVariable(varName);
        data.setValue(Expression.variable(valuePart));
        assignNode.setData(data);

        nodes.add(assignNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 解析函数调用语句
     */
    private StatementParseResult parseFunctionCallStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        // 提取函数名和参数
        int parenStart = statement.indexOf('(');
        String funcName = statement.substring(0, parenStart).trim();
        int parenEnd = findMatchingParen(statement, parenStart);
        String argsStr = statement.substring(parenStart + 1, parenEnd).trim();

        List<Expression> args = new ArrayList<>();
        if (!argsStr.isEmpty()) {
            String[] argParts = argsStr.split(",");
            for (String arg : argParts) {
                args.add(Expression.variable(arg.trim()));
            }
        }

        String nodeId = generateNodeId();
        VisualNode funcNode = new VisualNode();
        funcNode.setId(nodeId);
        funcNode.setType("function_call");
        funcNode.setPosition(new Position(NODE_X_BASE, yPos));

        FunctionCallNodeData data = new FunctionCallNodeData();
        data.setLabel(funcName + "()");
        data.setFunctionName(funcName);
        data.setArguments(args);
        funcNode.setData(data);

        nodes.add(funcNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 解析表达式语句
     */
    private StatementParseResult parseExpressionStatement(String statement, int yPos) {
        List<VisualNode> nodes = new ArrayList<>();
        List<VisualEdge> edges = new ArrayList<>();

        String expr = statement.replace(";", "").trim();

        String nodeId = generateNodeId();
        VisualNode exprNode = new VisualNode();
        exprNode.setId(nodeId);
        exprNode.setType("expression");
        exprNode.setPosition(new Position(NODE_X_BASE, yPos));

        ExpressionNodeData data = new ExpressionNodeData();
        data.setLabel(expr);
        data.setExpression(Expression.variable(expr));
        exprNode.setData(data);

        nodes.add(exprNode);

        return new StatementParseResult(nodes, edges, nodeId, yPos);
    }

    /**
     * 创建起始节点
     */
    private VisualNode createStartNode() {
        String nodeId = generateNodeId();
        VisualNode startNode = new VisualNode();
        startNode.setId(nodeId);
        startNode.setType("start");
        startNode.setPosition(new Position(NODE_X_BASE, 50));

        StartNodeData data = new StartNodeData();
        data.setLabel("开始");
        startNode.setData(data);

        return startNode;
    }

    /**
     * 创建结束节点
     */
    private VisualNode createEndNode(int yPos) {
        String nodeId = generateNodeId();
        VisualNode endNode = new VisualNode();
        endNode.setId(nodeId);
        endNode.setType("end");
        endNode.setPosition(new Position(NODE_X_BASE, yPos));

        EndNodeData data = new EndNodeData();
        data.setLabel("结束");
        endNode.setData(data);

        return endNode;
    }

    /**
     * 创建边
     */
    private VisualEdge createEdge(String source, String target) {
        VisualEdge edge = new VisualEdge();
        edge.setId(generateEdgeId());
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
    }

    /**
     * 生成节点ID
     */
    private String generateNodeId() {
        return "node_" + (++nodeIdCounter);
    }

    /**
     * 生成边ID
     */
    private String generateEdgeId() {
        return "edge_" + (++edgeIdCounter);
    }

    /**
     * 查找匹配的括号
     */
    private int findMatchingParen(String str, int start) {
        int count = 1;
        for (int i = start + 1; i < str.length(); i++) {
            if (str.charAt(i) == '(') count++;
            else if (str.charAt(i) == ')') {
                count--;
                if (count == 0) return i;
            }
        }
        return str.length() - 1;
    }

    /**
     * 语句解析结果
     */
    private static class StatementParseResult {
        final List<VisualNode> nodes;
        final List<VisualEdge> edges;
        final String lastNodeId;
        final int lastY;

        StatementParseResult(List<VisualNode> nodes, List<VisualEdge> edges,
                            String lastNodeId, int lastY) {
            this.nodes = nodes;
            this.edges = edges;
            this.lastNodeId = lastNodeId;
            this.lastY = lastY;
        }
    }

    /**
     * 解析结果
     */
    public static class ParseResult {
        private final boolean success;
        private final VisualFlowSchema flow;
        private final String errorMessage;

        private ParseResult(boolean success, VisualFlowSchema flow, String errorMessage) {
            this.success = success;
            this.flow = flow;
            this.errorMessage = errorMessage;
        }

        public static ParseResult success(VisualFlowSchema flow) {
            return new ParseResult(true, flow, null);
        }

        public static ParseResult failure(String errorMessage) {
            return new ParseResult(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public VisualFlowSchema getFlow() {
            return flow;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

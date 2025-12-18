package com.ql.qlexpress.web.core.parser;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.aparser.QLParser;
import com.alibaba.qlexpress4.exception.QLSyntaxException;
import com.ql.qlexpress.web.model.visual.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QL脚本到可视化流程的解析器
 *
 * 将QL脚本文本解析为可视化流程图JSON结构
 * 支持完整的QL语法，包括：
 * - import语句
 * - 函数定义
 * - if/else if/else
 * - for/foreach/while循环
 * - try-catch-finally
 * - 类型转换、new表达式
 * - 方法调用链
 *
 * @author qlexpress
 */
@Slf4j
@Component
public class QLToVisualParser {

    private final Express4Runner runner;
    private final StatementParser statementParser;
    private final StatementParser.NodeIdGenerator nodeIdGenerator;
    private final StatementParser.EdgeIdGenerator edgeIdGenerator;

    /**
     * 节点Y坐标偏移
     */
    private static final int NODE_Y_OFFSET = 100;

    /**
     * 节点X坐标
     */
    private static final int NODE_X_BASE = 250;

    // import语句模式
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+([a-zA-Z_$][a-zA-Z0-9_$.]*);?$");

    public QLToVisualParser() {
        this.runner = new Express4Runner(InitOptions.builder().build());
        this.nodeIdGenerator = new StatementParser.NodeIdGenerator();
        this.edgeIdGenerator = new StatementParser.EdgeIdGenerator();
        this.statementParser = new StatementParser(nodeIdGenerator, edgeIdGenerator);
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
        nodeIdGenerator.reset();
        edgeIdGenerator.reset();

        try {
            // 首先验证脚本语法
            runner.check(script);

            // 解析语法树（用于语法验证）
            QLParser.ProgramContext programContext = runner.parseToSyntaxTree(script);

            // 构建流程定义
            VisualFlowSchema flow = new VisualFlowSchema();
            flow.setVersion("2.0");

            // 存储解析结果
            List<String> imports = new ArrayList<>();
            List<FunctionDefinition> functions = new ArrayList<>();
            List<VisualNode> mainFlowNodes = new ArrayList<>();
            List<VisualEdge> mainFlowEdges = new ArrayList<>();

            // 分割语句
            List<String> statements = splitTopLevelStatements(script);

            // 添加起始节点
            VisualNode startNode = createStartNode();
            mainFlowNodes.add(startNode);

            String prevNodeId = startNode.getId();
            int yPos = NODE_Y_OFFSET;

            // 解析每个语句
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                // 处理import语句
                Matcher importMatcher = IMPORT_PATTERN.matcher(trimmed);
                if (importMatcher.matches()) {
                    imports.add(importMatcher.group(1));
                    continue;
                }

                // 处理函数定义
                if (trimmed.startsWith("function ")) {
                    StatementParser.StatementParseResult result = statementParser.parseStatement(trimmed, yPos);
                    if (result != null && result.getFunctionDefinition() != null) {
                        functions.add(result.getFunctionDefinition());
                    }
                    continue;
                }

                // 处理普通语句
                yPos += NODE_Y_OFFSET;
                StatementParser.StatementParseResult result = statementParser.parseStatement(trimmed, yPos);

                if (result != null && !result.getNodes().isEmpty()) {
                    mainFlowNodes.addAll(result.getNodes());
                    mainFlowEdges.addAll(result.getEdges());

                    // 连接前一个节点到当前语句的第一个节点
                    String firstNodeId = result.getNodes().get(0).getId();
                    mainFlowEdges.add(createEdge(prevNodeId, firstNodeId));

                    // 更新前一个节点ID
                    prevNodeId = result.getLastNodeId();
                    yPos = result.getLastY();
                }
            }

            // 添加结束节点
            yPos += NODE_Y_OFFSET;
            VisualNode endNode = createEndNode(yPos);
            mainFlowNodes.add(endNode);
            mainFlowEdges.add(createEdge(prevNodeId, endNode.getId()));

            // 设置流程定义
            flow.setImports(imports);
            flow.setFunctions(functions);
            flow.setNodes(mainFlowNodes);
            flow.setEdges(mainFlowEdges);

            FlowMetadata metadata = new FlowMetadata();
            metadata.setName("解析的流程");
            metadata.setDescription("从QL脚本解析生成");
            flow.setMetadata(metadata);

            log.info("QL脚本解析成功，imports: {}个，functions: {}个，节点: {}个，边: {}条",
                    imports.size(), functions.size(), mainFlowNodes.size(), mainFlowEdges.size());

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
     * 分割顶层语句
     * 正确处理 if-else-if-else, try-catch-finally 等复合语句
     */
    private List<String> splitTopLevelStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        int parenDepth = 0;
        boolean inString = false;
        char stringChar = 0;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = (i + 1 < script.length()) ? script.charAt(i + 1) : 0;

            // 处理注释
            if (!inString) {
                if (!inBlockComment && c == '/' && next == '/') {
                    inLineComment = true;
                }
                if (inLineComment && c == '\n') {
                    inLineComment = false;
                    continue;
                }
                if (inLineComment) {
                    continue;
                }
                if (!inLineComment && c == '/' && next == '*') {
                    inBlockComment = true;
                    i++;
                    continue;
                }
                if (inBlockComment && c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                    continue;
                }
                if (inBlockComment) {
                    continue;
                }
            }

            // 处理字符串
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                current.append(c);
                continue;
            }
            if (inString) {
                current.append(c);
                if (c == stringChar && (current.length() < 2 || current.charAt(current.length() - 2) != '\\')) {
                    inString = false;
                }
                continue;
            }

            // 处理括号深度
            if (c == '{') braceDepth++;
            else if (c == '}') braceDepth--;
            else if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;

            current.append(c);

            // 语句结束条件（在顶层）
            if (braceDepth == 0 && parenDepth == 0) {
                if (c == ';') {
                    String stmt = current.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current = new StringBuilder();
                } else if (c == '}') {
                    // 检查后面是否跟着 else, catch, finally 等关键字
                    String remaining = script.substring(i + 1).trim();
                    if (startsWithContinuationKeyword(remaining)) {
                        // 继续累积，不分割
                        continue;
                    }
                    String stmt = current.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current = new StringBuilder();
                }
            }
        }

        // 处理剩余内容
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }

    /**
     * 检查是否以继续关键字开头（else, catch, finally）
     */
    private boolean startsWithContinuationKeyword(String text) {
        return text.startsWith("else") || text.startsWith("catch") || text.startsWith("finally");
    }

    /**
     * 创建起始节点
     */
    private VisualNode createStartNode() {
        String nodeId = nodeIdGenerator.generate();
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
        String nodeId = nodeIdGenerator.generate();
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
        edge.setId(edgeIdGenerator.generate());
        edge.setSource(source);
        edge.setTarget(target);
        return edge;
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

package com.ql.qlexpress.web.core.transpiler;

import com.ql.qlexpress.web.exception.TranspileException;
import com.ql.qlexpress.web.model.visual.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 可视化流程到QL脚本转译器
 *
 * @author qlexpress
 */
@Component
public class VisualToQLTranspiler {

    /**
     * 转译可视化流程为QL脚本
     */
    public TranspileResult transpile(VisualFlowSchema schema) {
        return transpile(schema, new TranspileOptions());
    }

    /**
     * 转译可视化流程为QL脚本（带选项）
     */
    public TranspileResult transpile(VisualFlowSchema schema, TranspileOptions options) {
        TranspileContext context = new TranspileContext(options);

        try {
            // 验证输入
            validateSchema(schema, context);

            // 构建图结构
            FlowGraph graph = FlowGraph.build(schema.getNodes(), schema.getEdges());

            // 验证图结构
            validateGraph(graph, context);

            // 生成代码
            StringBuilder sb = new StringBuilder();

            // 1. 生成import
            generateImports(schema.getImports(), sb, context);

            // 2. 生成全局变量
            generateVariables(schema.getVariables(), sb, context);

            // 3. 生成自定义函数
            generateFunctions(schema.getFunctions(), graph, sb, context);

            // 4. 生成主逻辑
            generateMainFlow(graph, sb, context);

            // 格式化输出
            String script = context.isFormat() ? sb.toString() : compactCode(sb.toString());

            // 计算行数
            int lineCount = countLines(script);

            // 构建统计信息
            TranspileStats stats = context.buildStats(
                    graph.getNodeCount(),
                    graph.getEdgeCount(),
                    lineCount
            );

            return TranspileResult.success(script, context.getSourceMap(), context.getWarnings(), stats);

        } catch (TranspileException e) {
            return TranspileResult.failure(e.getMessage(), context.getWarnings());
        } catch (Exception e) {
            context.addError(null, "INTERNAL_ERROR", "转译过程发生内部错误: " + e.getMessage());
            return TranspileResult.failure("转译失败: " + e.getMessage(), context.getWarnings());
        }
    }

    /**
     * 验证输入Schema
     */
    private void validateSchema(VisualFlowSchema schema, TranspileContext context) {
        if (schema == null) {
            throw new TranspileException("流程定义不能为空");
        }
        if (schema.getNodes() == null || schema.getNodes().isEmpty()) {
            throw new TranspileException("流程节点列表不能为空");
        }
    }

    /**
     * 验证图结构
     */
    private void validateGraph(FlowGraph graph, TranspileContext context) {
        // 检查起始节点
        if (!graph.hasStartNode()) {
            throw new TranspileException("流程缺少起始节点(start)");
        }

        // 检查不可达节点
        Set<String> reachable = graph.findReachableNodes();
        for (VisualNode node : graph.getAllNodes()) {
            if (!reachable.contains(node.getId()) && !"start".equals(node.getType())) {
                context.addWarning(node.getId(), "UNREACHABLE_NODE",
                        "节点 '" + node.getId() + "' 不可达");
            }
        }
    }

    /**
     * 生成import语句
     */
    private void generateImports(List<String> imports, StringBuilder sb, TranspileContext context) {
        if (imports == null || imports.isEmpty()) {
            return;
        }

        for (String importClass : imports) {
            sb.append("import ").append(importClass).append(";");
            appendNewLine(sb, context);
        }

        // 添加空行
        appendNewLine(sb, context);
    }

    /**
     * 生成全局变量声明
     */
    private void generateVariables(List<VariableDefinition> variables, StringBuilder sb, TranspileContext context) {
        if (variables == null || variables.isEmpty()) {
            return;
        }

        if (context.isGenerateComments()) {
            sb.append(context.getIndent()).append("// 变量声明");
            appendNewLine(sb, context);
        }

        for (VariableDefinition var : variables) {
            sb.append(context.getIndent());

            // 类型声明（如果有）
            if (var.getType() != null && !var.getType().isEmpty()) {
                sb.append(var.getType()).append(" ");
            }

            sb.append(var.getName());

            // 初始值（如果有）
            if (var.getInitialValue() != null) {
                sb.append(" = ").append(var.getInitialValue().toQL());
            }

            sb.append(";");
            appendNewLine(sb, context);
        }

        // 添加空行
        appendNewLine(sb, context);
    }

    /**
     * 生成自定义函数
     */
    private void generateFunctions(List<FunctionDefinition> functions, FlowGraph graph,
                                   StringBuilder sb, TranspileContext context) {
        if (functions == null || functions.isEmpty()) {
            return;
        }

        for (FunctionDefinition func : functions) {
            if (context.isGenerateComments() && func.getDescription() != null) {
                sb.append(context.getIndent()).append("// ").append(func.getDescription());
                appendNewLine(sb, context);
            }

            // 函数签名
            sb.append(context.getIndent());
            if (func.getReturnType() != null && !func.getReturnType().isEmpty()) {
                sb.append(func.getReturnType()).append(" ");
            }
            sb.append("function ").append(func.getName()).append("(");

            // 参数列表
            if (func.getParameters() != null && !func.getParameters().isEmpty()) {
                List<String> params = new ArrayList<>();
                for (ParameterDefinition param : func.getParameters()) {
                    String paramStr = "";
                    if (param.getType() != null && !param.getType().isEmpty()) {
                        paramStr = param.getType() + " ";
                    }
                    paramStr += param.getName();
                    params.add(paramStr);
                }
                sb.append(String.join(", ", params));
            }

            sb.append(") {");
            appendNewLine(sb, context);

            // 函数体
            context.indent();
            if (func.getBodyEntrance() != null) {
                VisualNode bodyEntry = graph.getNode(func.getBodyEntrance());
                if (bodyEntry != null) {
                    Set<String> visited = new HashSet<>();
                    generateNodeCode(bodyEntry, graph, sb, context, visited);
                }
            }
            context.dedent();

            sb.append(context.getIndent()).append("}");
            appendNewLine(sb, context);
            appendNewLine(sb, context);
        }
    }

    /**
     * 生成主流程代码
     */
    private void generateMainFlow(FlowGraph graph, StringBuilder sb, TranspileContext context) {
        VisualNode startNode = graph.getStartNode();
        if (startNode == null) {
            return;
        }

        // 从起始节点的后继开始生成代码
        List<VisualNode> successors = graph.getSuccessors(startNode.getId());
        if (successors.isEmpty()) {
            context.addWarning(startNode.getId(), "EMPTY_FLOW", "起始节点没有后续节点");
            return;
        }

        Set<String> visited = new HashSet<>();
        visited.add(startNode.getId()); // 标记start节点为已访问

        for (VisualNode successor : successors) {
            generateNodeCode(successor, graph, sb, context, visited);
        }
    }

    /**
     * 生成单个节点的代码
     */
    private void generateNodeCode(VisualNode node, FlowGraph graph, StringBuilder sb,
                                  TranspileContext context, Set<String> visited) {
        if (node == null || visited.contains(node.getId())) {
            return;
        }
        visited.add(node.getId());

        // 记录源映射开始行
        int startLine = context.getCurrentLine();

        String nodeType = node.getType();
        NodeData data = node.getData();

        switch (nodeType) {
            case "start":
                // 起始节点不生成代码
                break;

            case "end":
                // 结束节点不生成代码
                break;

            case "expression":
                generateExpressionNode(node, (ExpressionNodeData) data, sb, context);
                break;

            case "assignment":
                generateAssignmentNode(node, (AssignmentNodeData) data, sb, context);
                break;

            case "function_call":
                generateFunctionCallNode(node, (FunctionCallNodeData) data, sb, context);
                break;

            case "if":
                generateIfNode(node, (IfNodeData) data, graph, sb, context, visited);
                return; // if节点自己处理后续节点

            case "for":
                generateForNode(node, (ForNodeData) data, graph, sb, context, visited);
                return; // for节点自己处理后续节点

            case "while":
                generateWhileNode(node, (WhileNodeData) data, graph, sb, context, visited);
                return; // while节点自己处理后续节点

            case "return":
                generateReturnNode(node, (ReturnNodeData) data, sb, context);
                break;

            default:
                context.addWarning(node.getId(), "UNKNOWN_NODE_TYPE",
                        "未知的节点类型: " + nodeType);
                break;
        }

        // 记录源映射
        int endLine = context.getCurrentLine();
        context.addSourceMapping(node.getId(), startLine, endLine);

        // 处理后续节点
        for (VisualNode successor : graph.getSuccessors(node.getId())) {
            generateNodeCode(successor, graph, sb, context, visited);
        }
    }

    /**
     * 生成表达式节点代码
     */
    private void generateExpressionNode(VisualNode node, ExpressionNodeData data,
                                        StringBuilder sb, TranspileContext context) {
        if (data == null || data.getExpression() == null) {
            context.addWarning(node.getId(), "EMPTY_EXPRESSION", "表达式节点缺少表达式");
            return;
        }

        sb.append(context.getIndent());

        if (data.getResultVariable() != null && !data.getResultVariable().isEmpty()) {
            sb.append(data.getResultVariable()).append(" = ");
        }

        sb.append(data.getExpression().toQL()).append(";");
        appendNewLine(sb, context);
    }

    /**
     * 生成赋值节点代码
     */
    private void generateAssignmentNode(VisualNode node, AssignmentNodeData data,
                                        StringBuilder sb, TranspileContext context) {
        if (data == null || data.getVariable() == null || data.getVariable().isEmpty()) {
            context.addWarning(node.getId(), "INVALID_ASSIGNMENT", "赋值节点缺少变量名");
            return;
        }

        sb.append(context.getIndent());
        sb.append(data.getVariable()).append(" = ");

        if (data.getValue() != null) {
            sb.append(data.getValue().toQL());
        } else {
            sb.append("null");
        }

        sb.append(";");
        appendNewLine(sb, context);
    }

    /**
     * 生成函数调用节点代码
     */
    private void generateFunctionCallNode(VisualNode node, FunctionCallNodeData data,
                                          StringBuilder sb, TranspileContext context) {
        if (data == null || data.getFunctionName() == null || data.getFunctionName().isEmpty()) {
            context.addWarning(node.getId(), "INVALID_FUNCTION_CALL", "函数调用节点缺少函数名");
            return;
        }

        sb.append(context.getIndent());

        if (data.getResultVariable() != null && !data.getResultVariable().isEmpty()) {
            sb.append(data.getResultVariable()).append(" = ");
        }

        sb.append(data.getFunctionName()).append("(");

        // 参数
        if (data.getArguments() != null && !data.getArguments().isEmpty()) {
            List<String> args = new ArrayList<>();
            for (Expression arg : data.getArguments()) {
                args.add(arg.toQL());
            }
            sb.append(String.join(", ", args));
        }

        sb.append(");");
        appendNewLine(sb, context);
    }

    /**
     * 生成IF节点代码
     */
    private void generateIfNode(VisualNode node, IfNodeData data, FlowGraph graph,
                                StringBuilder sb, TranspileContext context, Set<String> visited) {
        if (data == null || data.getCondition() == null) {
            context.addWarning(node.getId(), "INVALID_IF", "IF节点缺少条件表达式");
            return;
        }

        int startLine = context.getCurrentLine();

        // if 条件
        sb.append(context.getIndent()).append("if (").append(data.getCondition().toQL()).append(") {");
        appendNewLine(sb, context);

        // then分支
        context.indent();
        VisualNode thenNode = data.getThenBranch() != null ? graph.getNode(data.getThenBranch()) : null;
        if (thenNode == null) {
            // 尝试通过 sourceHandle 查找
            thenNode = graph.getSuccessorByHandle(node.getId(), "then");
        }
        if (thenNode != null) {
            Set<String> thenVisited = new HashSet<>(visited);
            generateNodeCode(thenNode, graph, sb, context, thenVisited);
        }
        context.dedent();

        // else分支
        VisualNode elseNode = data.getElseBranch() != null ? graph.getNode(data.getElseBranch()) : null;
        if (elseNode == null) {
            // 尝试通过 sourceHandle 查找
            elseNode = graph.getSuccessorByHandle(node.getId(), "else");
        }

        if (elseNode != null) {
            sb.append(context.getIndent()).append("} else {");
            appendNewLine(sb, context);

            context.indent();
            Set<String> elseVisited = new HashSet<>(visited);
            generateNodeCode(elseNode, graph, sb, context, elseVisited);
            context.dedent();
        }

        sb.append(context.getIndent()).append("}");
        appendNewLine(sb, context);

        // 记录源映射
        int endLine = context.getCurrentLine();
        context.addSourceMapping(node.getId(), startLine, endLine);

        // 处理if���点的默认后续（跳过then/else分支的出口）
        VisualNode nextNode = graph.getSuccessorByHandle(node.getId(), "next");
        if (nextNode != null && !visited.contains(nextNode.getId())) {
            generateNodeCode(nextNode, graph, sb, context, visited);
        }
    }

    /**
     * 生成FOR节点代码
     */
    private void generateForNode(VisualNode node, ForNodeData data, FlowGraph graph,
                                 StringBuilder sb, TranspileContext context, Set<String> visited) {
        if (data == null) {
            context.addWarning(node.getId(), "INVALID_FOR", "FOR节点数据为空");
            return;
        }

        int startLine = context.getCurrentLine();

        // for 循环头
        sb.append(context.getIndent()).append("for (");

        // 初始化
        if (data.getInit() != null) {
            sb.append(data.getInit().toQL());
        }
        sb.append("; ");

        // 条件
        if (data.getCondition() != null) {
            sb.append(data.getCondition().toQL());
        }
        sb.append("; ");

        // 更新
        if (data.getUpdate() != null) {
            sb.append(data.getUpdate().toQL());
        }
        sb.append(") {");
        appendNewLine(sb, context);

        // 循环体
        context.indent();
        VisualNode bodyNode = data.getBodyEntrance() != null ? graph.getNode(data.getBodyEntrance()) : null;
        if (bodyNode == null) {
            // 尝试通过 sourceHandle 查找
            bodyNode = graph.getSuccessorByHandle(node.getId(), "body");
        }
        if (bodyNode != null) {
            Set<String> bodyVisited = new HashSet<>(visited);
            generateNodeCode(bodyNode, graph, sb, context, bodyVisited);
        }
        context.dedent();

        sb.append(context.getIndent()).append("}");
        appendNewLine(sb, context);

        // 记录源映射
        int endLine = context.getCurrentLine();
        context.addSourceMapping(node.getId(), startLine, endLine);

        // 处理for节点的后续节点
        VisualNode nextNode = graph.getSuccessorByHandle(node.getId(), "next");
        if (nextNode != null && !visited.contains(nextNode.getId())) {
            generateNodeCode(nextNode, graph, sb, context, visited);
        }
    }

    /**
     * 生成WHILE节点代码
     */
    private void generateWhileNode(VisualNode node, WhileNodeData data, FlowGraph graph,
                                   StringBuilder sb, TranspileContext context, Set<String> visited) {
        if (data == null || data.getCondition() == null) {
            context.addWarning(node.getId(), "INVALID_WHILE", "WHILE节点缺少条件表达式");
            return;
        }

        int startLine = context.getCurrentLine();

        // while 循环头
        sb.append(context.getIndent()).append("while (").append(data.getCondition().toQL()).append(") {");
        appendNewLine(sb, context);

        // 循环体
        context.indent();
        VisualNode bodyNode = data.getBodyEntrance() != null ? graph.getNode(data.getBodyEntrance()) : null;
        if (bodyNode == null) {
            // 尝试通过 sourceHandle 查找
            bodyNode = graph.getSuccessorByHandle(node.getId(), "body");
        }
        if (bodyNode != null) {
            Set<String> bodyVisited = new HashSet<>(visited);
            generateNodeCode(bodyNode, graph, sb, context, bodyVisited);
        }
        context.dedent();

        sb.append(context.getIndent()).append("}");
        appendNewLine(sb, context);

        // 记录源映射
        int endLine = context.getCurrentLine();
        context.addSourceMapping(node.getId(), startLine, endLine);

        // 处理while节点的后续节点
        VisualNode nextNode = graph.getSuccessorByHandle(node.getId(), "next");
        if (nextNode != null && !visited.contains(nextNode.getId())) {
            generateNodeCode(nextNode, graph, sb, context, visited);
        }
    }

    /**
     * 生成RETURN节点代码
     */
    private void generateReturnNode(VisualNode node, ReturnNodeData data,
                                    StringBuilder sb, TranspileContext context) {
        sb.append(context.getIndent()).append("return");

        if (data != null && data.getValue() != null) {
            sb.append(" ").append(data.getValue().toQL());
        }

        sb.append(";");
        appendNewLine(sb, context);
    }

    /**
     * 添加换行
     */
    private void appendNewLine(StringBuilder sb, TranspileContext context) {
        if (context.isFormat()) {
            sb.append("\n");
        }
        context.incrementLine();
    }

    /**
     * 压缩代码（移除多余空白）
     */
    private String compactCode(String code) {
        return code.replaceAll("\\s+", " ").trim();
    }

    /**
     * 统计行数
     */
    private int countLines(String script) {
        if (script == null || script.isEmpty()) {
            return 0;
        }
        return script.split("\n").length;
    }
}

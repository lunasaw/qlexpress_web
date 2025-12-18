package com.ql.qlexpress.web.core.parser;

import com.ql.qlexpress.web.model.visual.*;
import com.ql.qlexpress.web.model.visual.TryCatchNodeData.CatchHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语句解析器
 * 将QL语句文本解析为节点和边结构
 *
 * 支持的语句类型:
 * - import语句
 * - 函数定义
 * - if/else if/else
 * - for循环
 * - foreach循环
 * - while循环
 * - try-catch-finally
 * - return语句
 * - break/continue
 * - 赋值语句
 * - 表达式语句
 *
 * @author qlexpress
 */
@Slf4j
public class StatementParser {

    private final ExpressionParser expressionParser;
    private final NodeIdGenerator nodeIdGenerator;
    private final EdgeIdGenerator edgeIdGenerator;

    // 语句模式
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+([a-zA-Z_$][a-zA-Z0-9_$.]*)\\s*;?$");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile(
            "^function\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*\\(([^)]*)\\)\\s*\\{",
            Pattern.DOTALL);
    private static final Pattern RETURN_PATTERN = Pattern.compile("^return\\s*(.*)\\s*;$", Pattern.DOTALL);
    private static final Pattern BREAK_PATTERN = Pattern.compile("^break\\s*;$");
    private static final Pattern CONTINUE_PATTERN = Pattern.compile("^continue\\s*;$");
    private static final Pattern THROW_PATTERN = Pattern.compile("^throw\\s+(.+)\\s*;$", Pattern.DOTALL);
    // foreach模式: for (Type var : collection) 或 for (var : collection)
    private static final Pattern FOREACH_PATTERN = Pattern.compile("^for\\s*\\([^;]*:[^;]*\\)");

    // 变量声明模式
    private static final Pattern VAR_DECL_PATTERN = Pattern.compile(
            "^([a-zA-Z_$][a-zA-Z0-9_$<>,\\s\\[\\]]*)\\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\\s*=\\s*(.+)\\s*;$",
            Pattern.DOTALL);
    private static final Pattern SIMPLE_ASSIGN_PATTERN = Pattern.compile(
            "^([a-zA-Z_$][a-zA-Z0-9_$.]*)\\s*=\\s*(.+)\\s*;$",
            Pattern.DOTALL);

    public StatementParser() {
        this.expressionParser = new ExpressionParser();
        this.nodeIdGenerator = new NodeIdGenerator();
        this.edgeIdGenerator = new EdgeIdGenerator();
    }

    public StatementParser(NodeIdGenerator nodeIdGenerator, EdgeIdGenerator edgeIdGenerator) {
        this.expressionParser = new ExpressionParser();
        this.nodeIdGenerator = nodeIdGenerator;
        this.edgeIdGenerator = edgeIdGenerator;
    }

    /**
     * 解析语句块
     *
     * @param statements 语句列表
     * @param yStart     起始Y坐标
     * @return 解析结果
     */
    public BlockParseResult parseStatements(List<String> statements, int yStart) {
        BlockParseResult result = new BlockParseResult();
        String prevNodeId = null;
        int yPos = yStart;

        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            StatementParseResult stmtResult = parseStatement(trimmed, yPos);
            if (stmtResult != null && !stmtResult.getNodes().isEmpty()) {
                result.getNodes().addAll(stmtResult.getNodes());
                result.getEdges().addAll(stmtResult.getEdges());

                // 连接到前一个节点
                if (prevNodeId != null) {
                    String firstNodeId = stmtResult.getNodes().get(0).getId();
                    result.getEdges().add(createEdge(prevNodeId, firstNodeId));
                }

                prevNodeId = stmtResult.getLastNodeId();
                yPos = stmtResult.getLastY() + 100;
            }
        }

        result.setLastNodeId(prevNodeId);
        result.setLastY(yPos);
        return result;
    }

    /**
     * 解析单个语句
     */
    public StatementParseResult parseStatement(String statement, int yPos) {
        String trimmed = statement.trim();

        // import语句
        Matcher importMatcher = IMPORT_PATTERN.matcher(trimmed);
        if (importMatcher.matches()) {
            return parseImportStatement(importMatcher.group(1), yPos);
        }

        // 函数定义
        if (trimmed.startsWith("function ")) {
            return parseFunctionDefinition(trimmed, yPos);
        }

        // if语句
        if (trimmed.startsWith("if")) {
            return parseIfStatement(trimmed, yPos);
        }

        // for循环
        if (trimmed.startsWith("for")) {
            // 检查是否是foreach
            Matcher foreachMatcher = FOREACH_PATTERN.matcher(trimmed);
            if (foreachMatcher.find()) {
                return parseForeachStatement(trimmed, yPos);
            }
            return parseForStatement(trimmed, yPos);
        }

        // while循环
        if (trimmed.startsWith("while")) {
            return parseWhileStatement(trimmed, yPos);
        }

        // try-catch
        if (trimmed.startsWith("try")) {
            return parseTryCatchStatement(trimmed, yPos);
        }

        // return语句
        Matcher returnMatcher = RETURN_PATTERN.matcher(trimmed);
        if (returnMatcher.matches()) {
            return parseReturnStatement(returnMatcher.group(1).trim(), yPos);
        }

        // break语句
        if (BREAK_PATTERN.matcher(trimmed).matches()) {
            return parseBreakStatement(yPos);
        }

        // continue语句
        if (CONTINUE_PATTERN.matcher(trimmed).matches()) {
            return parseContinueStatement(yPos);
        }

        // throw语句
        Matcher throwMatcher = THROW_PATTERN.matcher(trimmed);
        if (throwMatcher.matches()) {
            return parseThrowStatement(throwMatcher.group(1).trim(), yPos);
        }

        // 变量声明赋值
        Matcher varDeclMatcher = VAR_DECL_PATTERN.matcher(trimmed);
        if (varDeclMatcher.matches()) {
            return parseVariableDeclaration(
                    varDeclMatcher.group(1).trim(),
                    varDeclMatcher.group(2).trim(),
                    varDeclMatcher.group(3).trim(),
                    yPos);
        }

        // 简单赋值
        Matcher simpleAssignMatcher = SIMPLE_ASSIGN_PATTERN.matcher(trimmed);
        if (simpleAssignMatcher.matches()) {
            return parseSimpleAssignment(
                    simpleAssignMatcher.group(1).trim(),
                    simpleAssignMatcher.group(2).trim(),
                    yPos);
        }

        // 默认作为表达式语句
        return parseExpressionStatement(trimmed, yPos);
    }

    /**
     * 解析import语句
     */
    private StatementParseResult parseImportStatement(String className, int yPos) {
        // import语句不生成节点，只返回空结果
        // import信息会被添加到VisualFlowSchema的imports列表
        StatementParseResult result = new StatementParseResult();
        result.setImportClass(className);
        return result;
    }

    /**
     * 解析函数定义
     */
    private StatementParseResult parseFunctionDefinition(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        Matcher funcMatcher = FUNCTION_PATTERN.matcher(statement);
        if (!funcMatcher.find()) {
            return parseExpressionStatement(statement, yPos);
        }

        String funcName = funcMatcher.group(1);
        String paramsStr = funcMatcher.group(2).trim();

        // 解析参数
        List<ParameterDefinition> parameters = parseParameters(paramsStr);

        // 提取函数体
        int bodyStart = statement.indexOf('{') + 1;
        int bodyEnd = findMatchingBrace(statement, bodyStart - 1);
        String bodyContent = statement.substring(bodyStart, bodyEnd).trim();

        // 解析函数体
        List<String> bodyStatements = splitStatements(bodyContent);
        BlockParseResult bodyResult = parseStatements(bodyStatements, 50);

        // 创建FunctionBody
        FunctionBody functionBody = FunctionBody.builder()
                .nodes(bodyResult.getNodes())
                .edges(bodyResult.getEdges())
                .build();

        if (!bodyResult.getNodes().isEmpty()) {
            functionBody.setEntranceNodeId(bodyResult.getNodes().get(0).getId());
        }

        // 创建FunctionDefinition
        FunctionDefinition funcDef = FunctionDefinition.builder()
                .name(funcName)
                .parameters(parameters)
                .body(functionBody)
                .build();

        result.setFunctionDefinition(funcDef);
        return result;
    }

    /**
     * 解析if语句（包含else if和else）
     */
    private StatementParseResult parseIfStatement(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        // 使用精确的括号匹配来提取条件
        int parenStart = statement.indexOf('(');
        if (parenStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int parenEnd = findMatchingParen(statement, parenStart);
        if (parenEnd == -1) {
            return parseExpressionStatement(statement, yPos);
        }

        String condition = statement.substring(parenStart + 1, parenEnd).trim();
        Expression condExpr = expressionParser.parse(condition);

        // 创建if节点
        String nodeId = nodeIdGenerator.generate();
        VisualNode ifNode = new VisualNode();
        ifNode.setId(nodeId);
        ifNode.setType("if");
        ifNode.setPosition(new Position(250, yPos));

        IfNodeData data = new IfNodeData();
        data.setLabel("if (" + condition + ")");
        data.setCondition(condExpr);

        // 解析then分支 - 找到条件后面的 {
        int thenStart = statement.indexOf('{', parenEnd);
        if (thenStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int thenEnd = findMatchingBrace(statement, thenStart);
        String thenContent = statement.substring(thenStart + 1, thenEnd).trim();

        List<String> thenStatements = splitStatements(thenContent);
        BlockParseResult thenResult = parseStatements(thenStatements, yPos + 100);

        if (!thenResult.getNodes().isEmpty()) {
            result.getNodes().addAll(thenResult.getNodes());
            result.getEdges().addAll(thenResult.getEdges());
            data.setThenBranch(thenResult.getNodes().get(0).getId());
        }

        // 继续解析else if和else
        String remaining = statement.substring(thenEnd + 1).trim();
        List<ElseIfBranch> elseIfBranches = new ArrayList<>();
        int branchY = thenResult.getLastY() + 100;

        while (remaining.startsWith("else")) {
            // 跳过 "else" 关键字后的空白
            String afterElse = remaining.substring(4).trim();

            // 检查是否是 else if
            if (afterElse.startsWith("if")) {
                // else if 分支
                int elseIfParenStart = remaining.indexOf('(');
                int elseIfParenEnd = findMatchingParen(remaining, elseIfParenStart);
                String elseIfCond = remaining.substring(elseIfParenStart + 1, elseIfParenEnd).trim();
                Expression elseIfExpr = expressionParser.parse(elseIfCond);

                // 找到else if的body
                int elseIfBodyStart = remaining.indexOf('{', elseIfParenEnd);
                int elseIfBodyEnd = findMatchingBrace(remaining, elseIfBodyStart);
                String elseIfContent = remaining.substring(elseIfBodyStart + 1, elseIfBodyEnd).trim();

                List<String> elseIfStatements = splitStatements(elseIfContent);
                BlockParseResult elseIfResult = parseStatements(elseIfStatements, branchY);

                if (!elseIfResult.getNodes().isEmpty()) {
                    result.getNodes().addAll(elseIfResult.getNodes());
                    result.getEdges().addAll(elseIfResult.getEdges());

                    ElseIfBranch branch = new ElseIfBranch(elseIfExpr, elseIfResult.getNodes().get(0).getId());
                    elseIfBranches.add(branch);
                    branchY = elseIfResult.getLastY() + 100;
                }

                remaining = remaining.substring(elseIfBodyEnd + 1).trim();
            } else if (afterElse.startsWith("{")) {
                // 纯else分支
                int elseBodyStart = remaining.indexOf('{');
                int elseBodyEnd = findMatchingBrace(remaining, elseBodyStart);
                String elseContent = remaining.substring(elseBodyStart + 1, elseBodyEnd).trim();

                List<String> elseStatements = splitStatements(elseContent);
                BlockParseResult elseResult = parseStatements(elseStatements, branchY);

                if (!elseResult.getNodes().isEmpty()) {
                    result.getNodes().addAll(elseResult.getNodes());
                    result.getEdges().addAll(elseResult.getEdges());
                    data.setElseBranch(elseResult.getNodes().get(0).getId());
                    branchY = elseResult.getLastY() + 100;
                }
                break;
            } else {
                break;
            }
        }

        data.setElseIfBranches(elseIfBranches);
        ifNode.setData(data);
        result.getNodes().add(0, ifNode);

        result.setLastNodeId(nodeId);
        result.setLastY(branchY);

        return result;
    }

    /**
     * 查找匹配的右括号
     */
    private int findMatchingParen(String str, int start) {
        int count = 1;
        boolean inString = false;
        char stringChar = 0;

        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);

            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                continue;
            }
            if (inString) {
                if (c == stringChar && (i == 0 || str.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            if (c == '(') count++;
            else if (c == ')') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 解析for语句
     */
    private StatementParseResult parseForStatement(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        // 使用括号匹配提取 for 头部
        int parenStart = statement.indexOf('(');
        if (parenStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int parenEnd = findMatchingParen(statement, parenStart);
        if (parenEnd == -1) {
            return parseExpressionStatement(statement, yPos);
        }

        String forHeader = statement.substring(parenStart + 1, parenEnd);

        // 解析 init; condition; update
        String[] parts = splitForHeader(forHeader);
        if (parts.length != 3) {
            return parseExpressionStatement(statement, yPos);
        }

        String init = parts[0].trim();
        String condition = parts[1].trim();
        String update = parts[2].trim();

        Expression initExpr = expressionParser.parse(init);
        Expression condExpr = expressionParser.parse(condition);
        Expression updateExpr = expressionParser.parse(update);

        String nodeId = nodeIdGenerator.generate();
        VisualNode forNode = new VisualNode();
        forNode.setId(nodeId);
        forNode.setType("for");
        forNode.setPosition(new Position(250, yPos));

        ForNodeData data = new ForNodeData();
        data.setLabel("for (" + init + "; " + condition + "; " + update + ")");
        data.setInit(initExpr);
        data.setCondition(condExpr);
        data.setUpdate(updateExpr);

        // 解析循环体
        int bodyStart = statement.indexOf('{', parenEnd);
        if (bodyStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int bodyEnd = findMatchingBrace(statement, bodyStart);
        String bodyContent = statement.substring(bodyStart + 1, bodyEnd).trim();

        List<String> bodyStatements = splitStatements(bodyContent);
        BlockParseResult bodyResult = parseStatements(bodyStatements, yPos + 100);

        if (!bodyResult.getNodes().isEmpty()) {
            result.getNodes().addAll(bodyResult.getNodes());
            result.getEdges().addAll(bodyResult.getEdges());
            data.setBodyEntrance(bodyResult.getNodes().get(0).getId());
        }

        forNode.setData(data);
        result.getNodes().add(0, forNode);

        result.setLastNodeId(nodeId);
        result.setLastY(bodyResult.getLastY() + 100);

        return result;
    }

    /**
     * 分割 for 循环头部的三个部分
     */
    private String[] splitForHeader(String header) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenDepth = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < header.length(); i++) {
            char c = header.charAt(i);

            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                current.append(c);
                continue;
            }
            if (inString) {
                current.append(c);
                if (c == stringChar && (i == 0 || header.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            if (c == '(') parenDepth++;
            else if (c == ')') parenDepth--;

            if (c == ';' && parenDepth == 0) {
                parts.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts.toArray(new String[0]);
    }

    /**
     * 解析foreach语句
     */
    private StatementParseResult parseForeachStatement(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        // 使用括号匹配提取 foreach 头部
        int parenStart = statement.indexOf('(');
        if (parenStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int parenEnd = findMatchingParen(statement, parenStart);
        if (parenEnd == -1) {
            return parseExpressionStatement(statement, yPos);
        }

        String foreachHeader = statement.substring(parenStart + 1, parenEnd);

        // 查找 : 分隔符
        int colonPos = foreachHeader.indexOf(':');
        if (colonPos == -1) {
            return parseExpressionStatement(statement, yPos);
        }

        String leftPart = foreachHeader.substring(0, colonPos).trim();
        String collection = foreachHeader.substring(colonPos + 1).trim();

        // 解析左边的类型和变量名
        String[] tokens = leftPart.split("\\s+");
        String itemType;
        String itemName;
        if (tokens.length >= 2) {
            itemType = tokens[0];
            itemName = tokens[tokens.length - 1];
        } else {
            itemType = "";
            itemName = leftPart;
        }

        Expression collectionExpr = expressionParser.parse(collection);

        String nodeId = nodeIdGenerator.generate();
        VisualNode foreachNode = new VisualNode();
        foreachNode.setId(nodeId);
        foreachNode.setType("foreach");
        foreachNode.setPosition(new Position(250, yPos));

        ForeachNodeData data = new ForeachNodeData();
        String label = itemType.isEmpty() ?
                "for (" + itemName + " : " + collection + ")" :
                "for (" + itemType + " " + itemName + " : " + collection + ")";
        data.setLabel(label);
        data.setIteratorVariable(itemName);
        data.setIterable(collectionExpr);

        // 解析循环体
        int bodyStart = statement.indexOf('{', parenEnd);
        if (bodyStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int bodyEnd = findMatchingBrace(statement, bodyStart);
        String bodyContent = statement.substring(bodyStart + 1, bodyEnd).trim();

        List<String> bodyStatements = splitStatements(bodyContent);
        BlockParseResult bodyResult = parseStatements(bodyStatements, yPos + 100);

        if (!bodyResult.getNodes().isEmpty()) {
            result.getNodes().addAll(bodyResult.getNodes());
            result.getEdges().addAll(bodyResult.getEdges());
            data.setBodyBranch(bodyResult.getNodes().get(0).getId());
        }

        foreachNode.setData(data);
        result.getNodes().add(0, foreachNode);

        result.setLastNodeId(nodeId);
        result.setLastY(bodyResult.getLastY() + 100);

        return result;
    }

    /**
     * 解析while语句
     */
    private StatementParseResult parseWhileStatement(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        // 使用括号匹配提取 while 条件
        int parenStart = statement.indexOf('(');
        if (parenStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int parenEnd = findMatchingParen(statement, parenStart);
        if (parenEnd == -1) {
            return parseExpressionStatement(statement, yPos);
        }

        String condition = statement.substring(parenStart + 1, parenEnd).trim();
        Expression condExpr = expressionParser.parse(condition);

        String nodeId = nodeIdGenerator.generate();
        VisualNode whileNode = new VisualNode();
        whileNode.setId(nodeId);
        whileNode.setType("while");
        whileNode.setPosition(new Position(250, yPos));

        WhileNodeData data = new WhileNodeData();
        data.setLabel("while (" + condition + ")");
        data.setCondition(condExpr);

        // 解析循环体
        int bodyStart = statement.indexOf('{', parenEnd);
        if (bodyStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int bodyEnd = findMatchingBrace(statement, bodyStart);
        String bodyContent = statement.substring(bodyStart + 1, bodyEnd).trim();

        List<String> bodyStatements = splitStatements(bodyContent);
        BlockParseResult bodyResult = parseStatements(bodyStatements, yPos + 100);

        if (!bodyResult.getNodes().isEmpty()) {
            result.getNodes().addAll(bodyResult.getNodes());
            result.getEdges().addAll(bodyResult.getEdges());
            data.setBodyEntrance(bodyResult.getNodes().get(0).getId());
        }

        whileNode.setData(data);
        result.getNodes().add(0, whileNode);

        result.setLastNodeId(nodeId);
        result.setLastY(bodyResult.getLastY() + 100);

        return result;
    }

    /**
     * 解析try-catch语句
     */
    private StatementParseResult parseTryCatchStatement(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode tryCatchNode = new VisualNode();
        tryCatchNode.setId(nodeId);
        tryCatchNode.setType("try_catch");
        tryCatchNode.setPosition(new Position(250, yPos));

        TryCatchNodeData data = new TryCatchNodeData();
        data.setLabel("try-catch");

        // 解析try块
        int tryStart = statement.indexOf('{');
        if (tryStart == -1) {
            return parseExpressionStatement(statement, yPos);
        }
        int tryEnd = findMatchingBrace(statement, tryStart);
        String tryContent = statement.substring(tryStart + 1, tryEnd).trim();

        List<String> tryStatements = splitStatements(tryContent);
        BlockParseResult tryResult = parseStatements(tryStatements, yPos + 100);

        if (!tryResult.getNodes().isEmpty()) {
            result.getNodes().addAll(tryResult.getNodes());
            result.getEdges().addAll(tryResult.getEdges());
            data.setTryBranch(tryResult.getNodes().get(0).getId());
        }

        // 解析catch块
        String remaining = statement.substring(tryEnd + 1).trim();
        List<CatchHandler> catchHandlers = new ArrayList<>();
        int branchY = tryResult.getLastY() + 100;

        while (remaining.startsWith("catch")) {
            // 解析 catch (ExceptionType varName)
            int catchParenStart = remaining.indexOf('(');
            if (catchParenStart == -1) {
                break;
            }
            int catchParenEnd = findMatchingParen(remaining, catchParenStart);
            if (catchParenEnd == -1) {
                break;
            }

            // 解析异常类型和变量名
            String catchParams = remaining.substring(catchParenStart + 1, catchParenEnd).trim();
            String exceptionType = "";
            String exceptionVar = catchParams;

            // 分割类型和变量名
            int lastSpace = catchParams.lastIndexOf(' ');
            if (lastSpace > 0) {
                exceptionType = catchParams.substring(0, lastSpace).trim();
                exceptionVar = catchParams.substring(lastSpace + 1).trim();
            }

            // 解析catch体
            int catchBodyStart = remaining.indexOf('{', catchParenEnd);
            if (catchBodyStart == -1) {
                break;
            }
            int catchBodyEnd = findMatchingBrace(remaining, catchBodyStart);
            String catchContent = remaining.substring(catchBodyStart + 1, catchBodyEnd).trim();

            List<String> catchStatements = splitStatements(catchContent);
            BlockParseResult catchResult = parseStatements(catchStatements, branchY);

            CatchHandler handler = new CatchHandler();
            handler.setExceptionType(exceptionType);
            handler.setExceptionVariable(exceptionVar);
            if (!catchResult.getNodes().isEmpty()) {
                result.getNodes().addAll(catchResult.getNodes());
                result.getEdges().addAll(catchResult.getEdges());
                handler.setCatchBranch(catchResult.getNodes().get(0).getId());
                branchY = catchResult.getLastY() + 100;
            }
            catchHandlers.add(handler);

            remaining = remaining.substring(catchBodyEnd + 1).trim();
        }
        data.setCatchHandlers(catchHandlers);

        // 解析finally块
        if (remaining.startsWith("finally")) {
            int finallyBodyStart = remaining.indexOf('{');
            if (finallyBodyStart != -1) {
                int finallyBodyEnd = findMatchingBrace(remaining, finallyBodyStart);
                String finallyContent = remaining.substring(finallyBodyStart + 1, finallyBodyEnd).trim();

                List<String> finallyStatements = splitStatements(finallyContent);
                BlockParseResult finallyResult = parseStatements(finallyStatements, branchY);

                if (!finallyResult.getNodes().isEmpty()) {
                    result.getNodes().addAll(finallyResult.getNodes());
                    result.getEdges().addAll(finallyResult.getEdges());
                    data.setFinallyBranch(finallyResult.getNodes().get(0).getId());
                    branchY = finallyResult.getLastY() + 100;
                }
            }
        }

        tryCatchNode.setData(data);
        result.getNodes().add(0, tryCatchNode);

        result.setLastNodeId(nodeId);
        result.setLastY(branchY);

        return result;
    }

    /**
     * 解析return语句
     */
    private StatementParseResult parseReturnStatement(String returnValue, int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode returnNode = new VisualNode();
        returnNode.setId(nodeId);
        returnNode.setType("return");
        returnNode.setPosition(new Position(250, yPos));

        ReturnNodeData data = new ReturnNodeData();
        data.setLabel("return" + (returnValue.isEmpty() ? "" : " " + returnValue));
        if (!returnValue.isEmpty()) {
            data.setValue(expressionParser.parse(returnValue));
        }

        returnNode.setData(data);
        result.getNodes().add(returnNode);
        result.setLastNodeId(nodeId);
        result.setLastY(yPos);

        return result;
    }

    /**
     * 解析break语句
     */
    private StatementParseResult parseBreakStatement(int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode breakNode = new VisualNode();
        breakNode.setId(nodeId);
        breakNode.setType("break");
        breakNode.setPosition(new Position(250, yPos));

        BreakNodeData data = new BreakNodeData();
        data.setLabel("break");
        breakNode.setData(data);

        result.getNodes().add(breakNode);
        result.setLastNodeId(nodeId);
        result.setLastY(yPos);

        return result;
    }

    /**
     * 解析continue语句
     */
    private StatementParseResult parseContinueStatement(int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode continueNode = new VisualNode();
        continueNode.setId(nodeId);
        continueNode.setType("continue");
        continueNode.setPosition(new Position(250, yPos));

        ContinueNodeData data = new ContinueNodeData();
        data.setLabel("continue");
        continueNode.setData(data);

        result.getNodes().add(continueNode);
        result.setLastNodeId(nodeId);
        result.setLastY(yPos);

        return result;
    }

    /**
     * 解析throw语句
     */
    private StatementParseResult parseThrowStatement(String exceptionExpr, int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode throwNode = new VisualNode();
        throwNode.setId(nodeId);
        throwNode.setType("throw");
        throwNode.setPosition(new Position(250, yPos));

        ThrowNodeData data = new ThrowNodeData();
        data.setLabel("throw " + exceptionExpr);
        data.setException(expressionParser.parse(exceptionExpr));
        throwNode.setData(data);

        result.getNodes().add(throwNode);
        result.setLastNodeId(nodeId);
        result.setLastY(yPos);

        return result;
    }

    /**
     * 解析变量声明赋值
     */
    private StatementParseResult parseVariableDeclaration(String varType, String varName, String value, int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode assignNode = new VisualNode();
        assignNode.setId(nodeId);
        assignNode.setType("assignment");
        assignNode.setPosition(new Position(250, yPos));

        AssignmentNodeData data = new AssignmentNodeData();
        data.setLabel(varType + " " + varName + " = ...");
        data.setVariable(varName);
        data.setVariableType(varType);
        data.setValue(expressionParser.parse(value));
        assignNode.setData(data);

        result.getNodes().add(assignNode);
        result.setLastNodeId(nodeId);
        result.setLastY(yPos);

        return result;
    }

    /**
     * 解析简单赋值
     */
    private StatementParseResult parseSimpleAssignment(String varName, String value, int yPos) {
        StatementParseResult result = new StatementParseResult();

        String nodeId = nodeIdGenerator.generate();
        VisualNode assignNode = new VisualNode();
        assignNode.setId(nodeId);
        assignNode.setType("assignment");
        assignNode.setPosition(new Position(250, yPos));

        AssignmentNodeData data = new AssignmentNodeData();
        data.setLabel(varName + " = ...");
        data.setVariable(varName);
        data.setValue(expressionParser.parse(value));
        assignNode.setData(data);

        result.getNodes().add(assignNode);
        result.setLastNodeId(nodeId);
        result.setLastY(yPos);

        return result;
    }

    /**
     * 解析表达式语句
     */
    private StatementParseResult parseExpressionStatement(String statement, int yPos) {
        StatementParseResult result = new StatementParseResult();

        String expr = statement.endsWith(";") ? statement.substring(0, statement.length() - 1).trim() : statement.trim();

        // 检查是否是方法调用
        if (expr.contains("(") && expr.endsWith(")")) {
            String nodeId = nodeIdGenerator.generate();
            VisualNode funcNode = new VisualNode();
            funcNode.setId(nodeId);
            funcNode.setType("function_call");
            funcNode.setPosition(new Position(250, yPos));

            FunctionCallNodeData data = new FunctionCallNodeData();
            data.setLabel(truncateLabel(expr));
            data.setExpression(expressionParser.parse(expr));
            funcNode.setData(data);

            result.getNodes().add(funcNode);
            result.setLastNodeId(nodeId);
            result.setLastY(yPos);
        } else {
            String nodeId = nodeIdGenerator.generate();
            VisualNode exprNode = new VisualNode();
            exprNode.setId(nodeId);
            exprNode.setType("expression");
            exprNode.setPosition(new Position(250, yPos));

            ExpressionNodeData data = new ExpressionNodeData();
            data.setLabel(truncateLabel(expr));
            data.setExpression(expressionParser.parse(expr));
            exprNode.setData(data);

            result.getNodes().add(exprNode);
            result.setLastNodeId(nodeId);
            result.setLastY(yPos);
        }

        return result;
    }

    /**
     * 解析参数列表
     */
    private List<ParameterDefinition> parseParameters(String paramsStr) {
        List<ParameterDefinition> params = new ArrayList<>();
        if (paramsStr == null || paramsStr.isEmpty()) {
            return params;
        }

        String[] parts = paramsStr.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                String[] tokens = trimmed.split("\\s+");
                if (tokens.length >= 2) {
                    String type = tokens[0];
                    String name = tokens[tokens.length - 1];
                    params.add(ParameterDefinition.builder()
                            .type(type)
                            .name(name)
                            .build());
                } else if (tokens.length == 1) {
                    params.add(ParameterDefinition.builder()
                            .name(tokens[0])
                            .build());
                }
            }
        }
        return params;
    }

    /**
     * 分割语句（考虑嵌套代码块）
     */
    private List<String> splitStatements(String code) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceDepth = 0;
        int parenDepth = 0;
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);

            // 处理字符串
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                current.append(c);
                continue;
            }
            if (inString) {
                current.append(c);
                if (c == stringChar && (i == 0 || code.charAt(i - 1) != '\\')) {
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

            // 语句结束条件
            if (braceDepth == 0 && parenDepth == 0) {
                if (c == ';') {
                    String stmt = current.toString().trim();
                    if (!stmt.isEmpty()) {
                        statements.add(stmt);
                    }
                    current = new StringBuilder();
                } else if (c == '}') {
                    // 检查后面是否跟着 else, catch, finally 等关键字
                    String remainingCode = code.substring(i + 1).trim();
                    if (startsWithContinuationKeyword(remainingCode)) {
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
        String remainingStmt = current.toString().trim();
        if (!remainingStmt.isEmpty()) {
            statements.add(remainingStmt);
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
     * 查找匹配的右大括号
     */
    private int findMatchingBrace(String str, int start) {
        int count = 1;
        boolean inString = false;
        char stringChar = 0;

        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);

            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringChar = c;
                continue;
            }
            if (inString) {
                if (c == stringChar && (i == 0 || str.charAt(i - 1) != '\\')) {
                    inString = false;
                }
                continue;
            }

            if (c == '{') count++;
            else if (c == '}') {
                count--;
                if (count == 0) return i;
            }
        }
        return str.length() - 1;
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
     * 截断标签
     */
    private String truncateLabel(String text) {
        if (text.length() > 50) {
            return text.substring(0, 47) + "...";
        }
        return text;
    }

    // ========== 内部类 ==========

    /**
     * 语句解析结果
     */
    @Data
    public static class StatementParseResult {
        private List<VisualNode> nodes = new ArrayList<>();
        private List<VisualEdge> edges = new ArrayList<>();
        private String lastNodeId;
        private int lastY;
        private String importClass;
        private FunctionDefinition functionDefinition;
    }

    /**
     * 代码块解析结果
     */
    @Data
    public static class BlockParseResult {
        private List<VisualNode> nodes = new ArrayList<>();
        private List<VisualEdge> edges = new ArrayList<>();
        private String lastNodeId;
        private int lastY;
    }

    /**
     * 节点ID生成器
     */
    public static class NodeIdGenerator {
        private int counter = 0;

        public String generate() {
            return "node_" + (++counter);
        }

        public void reset() {
            counter = 0;
        }
    }

    /**
     * 边ID生成器
     */
    public static class EdgeIdGenerator {
        private int counter = 0;

        public String generate() {
            return "edge_" + (++counter);
        }

        public void reset() {
            counter = 0;
        }
    }
}

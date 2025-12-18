package com.ql.qlexpress.web.core.parser;

import com.ql.qlexpress.web.model.visual.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表达式解析器
 * 将QL表达式文本解析为Expression模型
 *
 * 支持的表达式类型:
 * - 字面量: 数字、字符串、布尔值、null
 * - 变量: 标识符
 * - 运算符: 二元、一元、三元运算符
 * - 方法调用: obj.method(args)
 * - 静态方法调用: Class.method(args)
 * - 构造函数: new ClassName(args)
 * - 类型转换: (Type)expr
 * - 数组访问: arr[i]
 * - 字段访问: obj.field, Class.class
 *
 * @author qlexpress
 */
@Slf4j
public class ExpressionParser {

    // 字面量模式
    private static final Pattern INTEGER_PATTERN = Pattern.compile("^-?\\d+[lL]?$");
    private static final Pattern FLOAT_PATTERN = Pattern.compile("^-?\\d+\\.\\d*[fFdD]?$");
    private static final Pattern STRING_PATTERN = Pattern.compile("^\".*\"$|^'.*'$", Pattern.DOTALL);
    private static final Pattern BOOLEAN_PATTERN = Pattern.compile("^(true|false)$");
    private static final Pattern NULL_PATTERN = Pattern.compile("^null$");

    // 标识符模式
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_$][a-zA-Z0-9_$]*$");

    // new表达式模式
    private static final Pattern NEW_PATTERN = Pattern.compile("^new\\s+([a-zA-Z_$][a-zA-Z0-9_$.]*)\\s*\\((.*)\\)$", Pattern.DOTALL);

    // 类型转换模式
    private static final Pattern CAST_PATTERN = Pattern.compile("^\\(([a-zA-Z_$][a-zA-Z0-9_$.<>,\\s]*)\\)\\s*(.+)$", Pattern.DOTALL);

    // 三元运算符模式
    private static final Pattern TERNARY_PATTERN = Pattern.compile("^(.+)\\s*\\?\\s*(.+)\\s*:\\s*(.+)$", Pattern.DOTALL);

    // 二元运算符（按优先级排序，从低到高）
    private static final String[] BINARY_OPERATORS = {
            "||", "&&",                              // 逻辑运算符
            "|", "^", "&",                           // 位运算符
            "==", "!=", "===", "!==",                // 相等运算符
            "<=", ">=", "<", ">", "instanceof",      // 比较运算符
            "<<", ">>", ">>>",                       // 移位运算符
            "+", "-",                                // 加减运算符
            "*", "/", "%"                            // 乘除运算符
    };

    // 一元前缀运算符
    private static final String[] UNARY_PREFIX_OPERATORS = {"!", "~", "-", "+", "++", "--"};

    // 一元后缀运算符
    private static final String[] UNARY_POSTFIX_OPERATORS = {"++", "--"};

    /**
     * 解析表达式文本
     *
     * @param exprText 表达式文本
     * @return Expression对象
     */
    public Expression parse(String exprText) {
        if (exprText == null || exprText.trim().isEmpty()) {
            return null;
        }

        String text = exprText.trim();

        try {
            return parseExpression(text);
        } catch (Exception e) {
            log.warn("表达式解析失败，使用原始表达式: {}", text, e);
            return Expression.raw(text);
        }
    }

    /**
     * 核心解析方法
     */
    private Expression parseExpression(String text) {
        text = text.trim();

        // 去除外层括号（如果是完整包裹的）
        text = removeOuterParentheses(text);

        // 1. 检查null
        if (NULL_PATTERN.matcher(text).matches()) {
            return Expression.literal(null);
        }

        // 2. 检查布尔字面量
        if (BOOLEAN_PATTERN.matcher(text).matches()) {
            return Expression.literal(Boolean.parseBoolean(text));
        }

        // 3. 检查整数字面量
        if (INTEGER_PATTERN.matcher(text).matches()) {
            String numStr = text.replaceAll("[lL]$", "");
            if (text.endsWith("l") || text.endsWith("L")) {
                return Expression.literal(Long.parseLong(numStr));
            }
            return Expression.literal(Integer.parseInt(numStr));
        }

        // 4. 检查浮点数字面量
        if (FLOAT_PATTERN.matcher(text).matches()) {
            String numStr = text.replaceAll("[fFdD]$", "");
            if (text.endsWith("f") || text.endsWith("F")) {
                return Expression.literal(Float.parseFloat(numStr));
            }
            return Expression.literal(Double.parseDouble(numStr));
        }

        // 5. 检查字符串字面量
        if (STRING_PATTERN.matcher(text).matches()) {
            String value = text.substring(1, text.length() - 1);
            return Expression.literal(unescapeString(value));
        }

        // 6. 检查三元运算符
        Expression ternary = parseTernary(text);
        if (ternary != null) {
            return ternary;
        }

        // 7. 检查二元运算符（从低优先级到高优先级）
        Expression binary = parseBinaryOperator(text);
        if (binary != null) {
            return binary;
        }

        // 8. 检查一元前缀运算符
        Expression unary = parseUnaryPrefix(text);
        if (unary != null) {
            return unary;
        }

        // 9. 检查new表达式
        Expression newExpr = parseNewExpression(text);
        if (newExpr != null) {
            return newExpr;
        }

        // 10. 检查类型转换
        Expression cast = parseCastExpression(text);
        if (cast != null) {
            return cast;
        }

        // 11. 检查方法调用、字段访问、数组访问链
        Expression chain = parseChainExpression(text);
        if (chain != null) {
            return chain;
        }

        // 12. 简单标识符
        if (IDENTIFIER_PATTERN.matcher(text).matches()) {
            return Expression.variable(text);
        }

        // 13. 无法解析，返回原始表达式
        return Expression.raw(text);
    }

    /**
     * 解析三元运算符
     */
    private Expression parseTernary(String text) {
        // 找到最外层的 ? 和 :
        int questionMark = -1;
        int colon = -1;
        int depth = 0;
        int ternaryDepth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            } else if (c == '"' || c == '\'') {
                i = skipString(text, i);
            } else if (depth == 0) {
                if (c == '?' && questionMark == -1 && ternaryDepth == 0) {
                    questionMark = i;
                    ternaryDepth++;
                } else if (c == ':' && questionMark != -1 && ternaryDepth == 1) {
                    colon = i;
                    break;
                }
            }
        }

        if (questionMark > 0 && colon > questionMark) {
            String condition = text.substring(0, questionMark).trim();
            String thenPart = text.substring(questionMark + 1, colon).trim();
            String elsePart = text.substring(colon + 1).trim();

            if (!condition.isEmpty() && !thenPart.isEmpty() && !elsePart.isEmpty()) {
                return Expression.ternary(
                        parseExpression(condition),
                        parseExpression(thenPart),
                        parseExpression(elsePart)
                );
            }
        }

        return null;
    }

    /**
     * 解析二元运算符
     */
    private Expression parseBinaryOperator(String text) {
        // 从低优先级到高优先级查找运算符
        for (String op : BINARY_OPERATORS) {
            int opIndex = findOperator(text, op);
            if (opIndex > 0 && opIndex < text.length() - op.length()) {
                String left = text.substring(0, opIndex).trim();
                String right = text.substring(opIndex + op.length()).trim();

                if (!left.isEmpty() && !right.isEmpty()) {
                    return Expression.operator(op, parseExpression(left), parseExpression(right));
                }
            }
        }
        return null;
    }

    /**
     * 解析一元前缀运算符
     */
    private Expression parseUnaryPrefix(String text) {
        for (String op : UNARY_PREFIX_OPERATORS) {
            if (text.startsWith(op)) {
                String operand = text.substring(op.length()).trim();
                if (!operand.isEmpty()) {
                    return Expression.unaryOperator(op, parseExpression(operand));
                }
            }
        }
        return null;
    }

    /**
     * 解析new表达式
     */
    private Expression parseNewExpression(String text) {
        Matcher matcher = NEW_PATTERN.matcher(text);
        if (matcher.matches()) {
            String className = matcher.group(1).trim();
            String argsStr = matcher.group(2).trim();

            List<Expression> args = parseArgumentList(argsStr);
            return Expression.newInstance(className, args);
        }
        return null;
    }

    /**
     * 解析类型转换表达���
     */
    private Expression parseCastExpression(String text) {
        if (!text.startsWith("(")) {
            return null;
        }

        // 查找匹配的右括号
        int closeIndex = findMatchingParen(text, 0);
        if (closeIndex <= 0 || closeIndex >= text.length() - 1) {
            return null;
        }

        String typeCandidate = text.substring(1, closeIndex).trim();
        String restExpr = text.substring(closeIndex + 1).trim();

        // 验证是否是类型（不能包含运算符）
        if (isValidType(typeCandidate) && !restExpr.isEmpty()) {
            return Expression.cast(typeCandidate, parseExpression(restExpr));
        }

        return null;
    }

    /**
     * 解析链式表达式（方法调用、字段访问、数组访问）
     */
    private Expression parseChainExpression(String text) {
        // 首先找到基础部分（标识符或括号表达式）
        int pos = 0;
        Expression base = null;

        // 检查是否以标识符开头
        StringBuilder identBuilder = new StringBuilder();
        while (pos < text.length()) {
            char c = text.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '$') {
                identBuilder.append(c);
                pos++;
            } else {
                break;
            }
        }

        if (identBuilder.length() > 0) {
            String ident = identBuilder.toString();
            base = Expression.variable(ident);
        } else if (text.startsWith("(")) {
            int closeIndex = findMatchingParen(text, 0);
            if (closeIndex > 0) {
                base = parseExpression(text.substring(1, closeIndex));
                pos = closeIndex + 1;
            }
        }

        if (base == null) {
            return null;
        }

        // 继续解析链式操作
        while (pos < text.length()) {
            char c = text.charAt(pos);

            if (c == '.') {
                // 字段访问或方法调用
                pos++;
                StringBuilder memberBuilder = new StringBuilder();
                while (pos < text.length()) {
                    char mc = text.charAt(pos);
                    if (Character.isLetterOrDigit(mc) || mc == '_' || mc == '$') {
                        memberBuilder.append(mc);
                        pos++;
                    } else {
                        break;
                    }
                }

                String member = memberBuilder.toString();
                if (member.isEmpty()) {
                    return Expression.raw(text);
                }

                // 检查是否是方法调用
                if (pos < text.length() && text.charAt(pos) == '(') {
                    int closeIndex = findMatchingParen(text, pos);
                    if (closeIndex > pos) {
                        String argsStr = text.substring(pos + 1, closeIndex).trim();
                        List<Expression> args = parseArgumentList(argsStr);

                        // 判断是静态方法调用还是实例方法调用
                        if (base instanceof VariableExpression) {
                            String baseName = ((VariableExpression) base).getName();
                            if (Character.isUpperCase(baseName.charAt(0))) {
                                // 可能是静态方法调用
                                base = Expression.staticMethod(baseName, member, args);
                            } else {
                                base = Expression.method(base, member, args);
                            }
                        } else {
                            base = Expression.method(base, member, args);
                        }
                        pos = closeIndex + 1;
                    } else {
                        return Expression.raw(text);
                    }
                } else {
                    // 字段访问
                    base = Expression.fieldAccess(base, member);
                }
            } else if (c == '[') {
                // 数组访问
                int closeIndex = findMatchingBracket(text, pos);
                if (closeIndex > pos) {
                    String indexStr = text.substring(pos + 1, closeIndex).trim();
                    Expression index = parseExpression(indexStr);
                    base = Expression.arrayAccess(base, index);
                    pos = closeIndex + 1;
                } else {
                    return Expression.raw(text);
                }
            } else if (c == '(') {
                // 函数调用（基础变量作为函数名）
                int closeIndex = findMatchingParen(text, pos);
                if (closeIndex > pos && base instanceof VariableExpression) {
                    String funcName = ((VariableExpression) base).getName();
                    String argsStr = text.substring(pos + 1, closeIndex).trim();
                    List<Expression> args = parseArgumentList(argsStr);
                    base = Expression.function(funcName, args);
                    pos = closeIndex + 1;
                } else {
                    return Expression.raw(text);
                }
            } else {
                // 链式结束
                break;
            }
        }

        // 检查是否还有剩余部分
        if (pos < text.length()) {
            String remaining = text.substring(pos).trim();
            if (!remaining.isEmpty()) {
                // 可能是后缀运算符
                for (String op : UNARY_POSTFIX_OPERATORS) {
                    if (remaining.equals(op)) {
                        return Expression.unaryOperator(op + "_postfix", base);
                    }
                }
                // 无法完全解析，返回原始表达式
                return Expression.raw(text);
            }
        }

        return base;
    }

    /**
     * 解析参数列表
     */
    private List<Expression> parseArgumentList(String argsStr) {
        List<Expression> args = new ArrayList<>();
        if (argsStr == null || argsStr.isEmpty()) {
            return args;
        }

        List<String> argParts = splitArguments(argsStr);
        for (String arg : argParts) {
            String trimmed = arg.trim();
            if (!trimmed.isEmpty()) {
                args.add(parseExpression(trimmed));
            }
        }
        return args;
    }

    /**
     * 分割参数列表（考虑嵌套括号和字符串）
     */
    private List<String> splitArguments(String argsStr) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;

        for (int i = 0; i < argsStr.length(); i++) {
            char c = argsStr.charAt(i);
            if (c == '(' || c == '[' || c == '{' || c == '<') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}' || c == '>') {
                depth--;
            } else if (c == '"' || c == '\'') {
                i = skipString(argsStr, i);
            } else if (c == ',' && depth == 0) {
                result.add(argsStr.substring(start, i));
                start = i + 1;
            }
        }

        if (start < argsStr.length()) {
            result.add(argsStr.substring(start));
        }

        return result;
    }

    /**
     * 查找运算符位置（考虑嵌套和字符串）
     */
    private int findOperator(String text, String op) {
        int depth = 0;
        int searchStart = text.length() - 1;

        // 从右向左查找（因为运算符是左结合的）
        for (int i = searchStart; i >= op.length() - 1; i--) {
            char c = text.charAt(i);
            if (c == ')' || c == ']' || c == '}') {
                depth++;
            } else if (c == '(' || c == '[' || c == '{') {
                depth--;
            } else if (c == '"' || c == '\'') {
                // 向左跳过字符串
                i = skipStringBackward(text, i);
            } else if (depth == 0) {
                // 检查是否匹配运算符
                if (i >= op.length() - 1) {
                    String candidate = text.substring(i - op.length() + 1, i + 1);
                    if (candidate.equals(op)) {
                        // 确保不是更长运算符的一部分
                        boolean isLonger = false;
                        for (String longer : BINARY_OPERATORS) {
                            if (longer.length() > op.length() && longer.contains(op)) {
                                int startIdx = i - op.length() + 1;
                                for (int offset = 0; offset <= longer.length() - op.length(); offset++) {
                                    int checkStart = startIdx - offset;
                                    int checkEnd = checkStart + longer.length();
                                    if (checkStart >= 0 && checkEnd <= text.length()) {
                                        String check = text.substring(checkStart, checkEnd);
                                        if (check.equals(longer)) {
                                            isLonger = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (!isLonger) {
                            return i - op.length() + 1;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /**
     * 查找匹配的右括号
     */
    private int findMatchingParen(String text, int start) {
        int count = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipString(text, i);
            } else if (c == '(') {
                count++;
            } else if (c == ')') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 查找匹配的右方括号
     */
    private int findMatchingBracket(String text, int start) {
        int count = 1;
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\'') {
                i = skipString(text, i);
            } else if (c == '[') {
                count++;
            } else if (c == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 跳过字符串
     */
    private int skipString(String text, int start) {
        char quote = text.charAt(start);
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++; // 跳过转义字符
            } else if (c == quote) {
                return i;
            }
        }
        return text.length() - 1;
    }

    /**
     * 向左跳过字符串
     */
    private int skipStringBackward(String text, int end) {
        char quote = text.charAt(end);
        for (int i = end - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == quote) {
                // 检查是否转义
                int backslashCount = 0;
                for (int j = i - 1; j >= 0 && text.charAt(j) == '\\'; j--) {
                    backslashCount++;
                }
                if (backslashCount % 2 == 0) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * 去除外层括号
     */
    private String removeOuterParentheses(String text) {
        while (text.startsWith("(") && text.endsWith(")")) {
            int close = findMatchingParen(text, 0);
            if (close == text.length() - 1) {
                text = text.substring(1, text.length() - 1).trim();
            } else {
                break;
            }
        }
        return text;
    }

    /**
     * 检查是否是有效的类型名
     */
    private boolean isValidType(String type) {
        // 类型可以包含字母、数字、下划线、点、尖括号、逗号、空格
        return type.matches("[a-zA-Z_$][a-zA-Z0-9_$.<>,\\[\\]\\s]*");
    }

    /**
     * 反转义字符串
     */
    private String unescapeString(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\\\", "\\");
    }
}

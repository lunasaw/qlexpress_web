package com.ql.qlexpress.web.core.transpiler;

import com.ql.qlexpress.web.model.visual.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 表达式代码生成器
 * 将Expression模型转换为QL代码文本
 *
 * 支持所有表达式类型:
 * - 字面量、变量、运算符
 * - 方法调用、静态方法调用
 * - new表达式、类型转换
 * - 数组访问、字段访问
 * - 三元运算符
 *
 * @author qlexpress
 */
@Slf4j
public class ExpressionEmitter {

    /**
     * 生成表达式的QL代码
     *
     * @param expr 表达式
     * @return QL代码文本
     */
    public String emit(Expression expr) {
        if (expr == null) {
            return "";
        }

        // 使用多态调用toQL方法
        // 每个Expression子类都有自己的toQL实现
        try {
            return expr.toQL();
        } catch (Exception e) {
            log.warn("表达式生成失败，使用备用方案: {}", e.getMessage());
            return emitFallback(expr);
        }
    }

    /**
     * 备用生成方法（根据类型分别处理）
     */
    private String emitFallback(Expression expr) {
        if (expr instanceof LiteralExpression) {
            return emitLiteral((LiteralExpression) expr);
        }
        if (expr instanceof VariableExpression) {
            return emitVariable((VariableExpression) expr);
        }
        if (expr instanceof OperatorExpression) {
            return emitOperator((OperatorExpression) expr);
        }
        if (expr instanceof FunctionExpression) {
            return emitFunction((FunctionExpression) expr);
        }
        if (expr instanceof MethodExpression) {
            return emitMethod((MethodExpression) expr);
        }
        if (expr instanceof StaticMethodExpression) {
            return emitStaticMethod((StaticMethodExpression) expr);
        }
        if (expr instanceof NewExpression) {
            return emitNew((NewExpression) expr);
        }
        if (expr instanceof CastExpression) {
            return emitCast((CastExpression) expr);
        }
        if (expr instanceof FieldAccessExpression) {
            return emitFieldAccess((FieldAccessExpression) expr);
        }
        if (expr instanceof ArrayAccessExpression) {
            return emitArrayAccess((ArrayAccessExpression) expr);
        }
        if (expr instanceof TernaryExpression) {
            return emitTernary((TernaryExpression) expr);
        }
        if (expr instanceof RawExpression) {
            return emitRaw((RawExpression) expr);
        }

        log.warn("未知的表达式类型: {}", expr.getClass().getName());
        return expr.toString();
    }

    /**
     * 生成字面量
     */
    private String emitLiteral(LiteralExpression expr) {
        Object value = expr.getValue();
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }
        if (value instanceof Character) {
            return "'" + escapeChar((Character) value) + "'";
        }
        if (value instanceof Long) {
            return value.toString() + "L";
        }
        if (value instanceof Float) {
            return value.toString() + "f";
        }
        if (value instanceof Double) {
            return value.toString() + "d";
        }
        return value.toString();
    }

    /**
     * 生成变量引用
     */
    private String emitVariable(VariableExpression expr) {
        return expr.getName();
    }

    /**
     * 生成运算符表达式
     */
    private String emitOperator(OperatorExpression expr) {
        String op = expr.getOperator();
        Expression left = expr.getLeft();
        Expression right = expr.getRight();

        // 一元运算符
        if (right == null) {
            if (op.endsWith("_postfix")) {
                // 后缀运算符
                String realOp = op.replace("_postfix", "");
                return emit(left) + realOp;
            }
            // 前缀运算符
            return op + emit(left);
        }

        // 二元运算符
        String leftStr = emit(left);
        String rightStr = emit(right);

        // 对于复杂的子表达式添加括号
        if (needsParentheses(left, op)) {
            leftStr = "(" + leftStr + ")";
        }
        if (needsParentheses(right, op)) {
            rightStr = "(" + rightStr + ")";
        }

        return leftStr + " " + op + " " + rightStr;
    }

    /**
     * 判断是否需要括号
     */
    private boolean needsParentheses(Expression expr, String parentOp) {
        if (expr instanceof OperatorExpression) {
            OperatorExpression opExpr = (OperatorExpression) expr;
            return getPrecedence(opExpr.getOperator()) < getPrecedence(parentOp);
        }
        if (expr instanceof TernaryExpression) {
            return true;
        }
        return false;
    }

    /**
     * 获取运算符优先级
     */
    private int getPrecedence(String op) {
        switch (op) {
            case "||":
                return 1;
            case "&&":
                return 2;
            case "|":
                return 3;
            case "^":
                return 4;
            case "&":
                return 5;
            case "==":
            case "!=":
            case "===":
            case "!==":
                return 6;
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "instanceof":
                return 7;
            case "<<":
            case ">>":
            case ">>>":
                return 8;
            case "+":
            case "-":
                return 9;
            case "*":
            case "/":
            case "%":
                return 10;
            default:
                return 0;
        }
    }

    /**
     * 生成函数调用
     */
    private String emitFunction(FunctionExpression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(expr.getFunctionName());
        sb.append("(");
        sb.append(emitArguments(expr.getArguments()));
        sb.append(")");
        return sb.toString();
    }

    /**
     * 生成方法调用
     */
    private String emitMethod(MethodExpression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(emit(expr.getTarget()));
        sb.append(".");
        sb.append(expr.getMethodName());
        sb.append("(");
        sb.append(emitArguments(expr.getArguments()));
        sb.append(")");
        return sb.toString();
    }

    /**
     * 生成静态方法调用
     */
    private String emitStaticMethod(StaticMethodExpression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(expr.getClassName());
        sb.append(".");
        sb.append(expr.getMethodName());
        sb.append("(");
        sb.append(emitArguments(expr.getArguments()));
        sb.append(")");
        return sb.toString();
    }

    /**
     * 生成new表达式
     */
    private String emitNew(NewExpression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append("new ");
        sb.append(expr.getClassName());
        sb.append("(");
        sb.append(emitArguments(expr.getArguments()));
        sb.append(")");
        return sb.toString();
    }

    /**
     * 生成类型转换
     */
    private String emitCast(CastExpression expr) {
        return "(" + expr.getTargetType() + ")" + emit(expr.getExpression());
    }

    /**
     * 生成字段访问
     */
    private String emitFieldAccess(FieldAccessExpression expr) {
        return emit(expr.getObject()) + "." + expr.getField();
    }

    /**
     * 生成数组访问
     */
    private String emitArrayAccess(ArrayAccessExpression expr) {
        return emit(expr.getArray()) + "[" + emit(expr.getIndex()) + "]";
    }

    /**
     * 生成三元运算符
     */
    private String emitTernary(TernaryExpression expr) {
        return "(" + emit(expr.getCondition()) + " ? " +
                emit(expr.getThenExpression()) + " : " +
                emit(expr.getElseExpression()) + ")";
    }

    /**
     * 生成原始表达式
     */
    private String emitRaw(RawExpression expr) {
        return expr.getCode();
    }

    /**
     * 生成参数列表
     */
    private String emitArguments(List<Expression> args) {
        if (args == null || args.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(emit(args.get(i)));
        }
        return sb.toString();
    }

    /**
     * 转义字符串
     */
    private String escapeString(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * 转义字符
     */
    private String escapeChar(char c) {
        switch (c) {
            case '\\':
                return "\\\\";
            case '\'':
                return "\\'";
            case '\n':
                return "\\n";
            case '\r':
                return "\\r";
            case '\t':
                return "\\t";
            default:
                return String.valueOf(c);
        }
    }
}

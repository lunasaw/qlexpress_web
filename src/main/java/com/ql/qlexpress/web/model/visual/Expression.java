package com.ql.qlexpress.web.model.visual;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

/**
 * 表达式基类
 *
 * @author qlexpress
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LiteralExpression.class, name = "literal"),
        @JsonSubTypes.Type(value = VariableExpression.class, name = "variable"),
        @JsonSubTypes.Type(value = OperatorExpression.class, name = "operator"),
        @JsonSubTypes.Type(value = FunctionExpression.class, name = "function"),
        @JsonSubTypes.Type(value = MethodExpression.class, name = "method"),
        @JsonSubTypes.Type(value = RawExpression.class, name = "raw"),
})
public abstract class Expression {

    /**
     * 转换为QL脚本
     */
    public abstract String toQL();

    /**
     * 创建字面量表达式
     */
    public static Expression literal(Object value) {
        return new LiteralExpression(value);
    }

    /**
     * 创建变量表达式
     */
    public static Expression variable(String name) {
        return new VariableExpression(name);
    }

    /**
     * 创建二元操作符表达式
     */
    public static Expression operator(String op, Expression left, Expression right) {
        return new OperatorExpression(op, left, right);
    }

    /**
     * 创建一元操作符表达式
     */
    public static Expression unaryOperator(String op, Expression operand) {
        return new OperatorExpression(op, operand, null);
    }

    /**
     * 创建函数调用表达式
     */
    public static Expression function(String name, List<Expression> args) {
        return new FunctionExpression(name, args);
    }

    /**
     * 创建方法调用表达式
     */
    public static Expression method(Expression target, String name, List<Expression> args) {
        return new MethodExpression(target, name, args);
    }

    /**
     * 创建原始代码表达式
     */
    public static Expression raw(String code) {
        return new RawExpression(code);
    }
}

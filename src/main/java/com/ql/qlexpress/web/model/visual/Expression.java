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
        @JsonSubTypes.Type(value = StaticMethodExpression.class, name = "static_method"),
        @JsonSubTypes.Type(value = NewExpression.class, name = "new"),
        @JsonSubTypes.Type(value = CastExpression.class, name = "cast"),
        @JsonSubTypes.Type(value = FieldAccessExpression.class, name = "field_access"),
        @JsonSubTypes.Type(value = ArrayAccessExpression.class, name = "array_access"),
        @JsonSubTypes.Type(value = TernaryExpression.class, name = "ternary"),
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

    /**
     * 创建静态方法调用表达式
     */
    public static Expression staticMethod(String className, String methodName, List<Expression> args) {
        return new StaticMethodExpression(className, methodName, args);
    }

    /**
     * 创建new表达式
     */
    public static Expression newInstance(String className, List<Expression> args) {
        return new NewExpression(className, args);
    }

    /**
     * 创建类型转换表达式
     */
    public static Expression cast(String targetType, Expression expression) {
        return new CastExpression(targetType, expression);
    }

    /**
     * 创建字段访问表达式
     */
    public static Expression fieldAccess(Expression object, String field) {
        return new FieldAccessExpression(object, field);
    }

    /**
     * 创建数组访问表达式
     */
    public static Expression arrayAccess(Expression array, Expression index) {
        return new ArrayAccessExpression(array, index);
    }

    /**
     * 创建三元运算符表达式
     */
    public static Expression ternary(Expression condition, Expression thenExpr, Expression elseExpr) {
        return new TernaryExpression(condition, thenExpr, elseExpr);
    }
}

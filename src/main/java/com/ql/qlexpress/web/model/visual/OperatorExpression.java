package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 操作符表达式
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class OperatorExpression extends Expression {

    /**
     * 操作符
     */
    private String operator;

    /**
     * 左操作数（一元操作符时为操作数）
     */
    private Expression left;

    /**
     * 右操作数（一元操作符时为null）
     */
    private Expression right;

    /**
     * 是否为一元操作符
     */
    public boolean isUnary() {
        return right == null;
    }

    @Override
    public String toQL() {
        if (isUnary()) {
            // 一元操作符
            if (isPostfixOperator(operator)) {
                return left.toQL() + operator;
            } else {
                return operator + left.toQL();
            }
        } else {
            // 二元操作符
            return "(" + left.toQL() + " " + operator + " " + right.toQL() + ")";
        }
    }

    /**
     * 判断是否为后缀操作符
     */
    private boolean isPostfixOperator(String op) {
        return "++".equals(op) || "--".equals(op);
    }
}

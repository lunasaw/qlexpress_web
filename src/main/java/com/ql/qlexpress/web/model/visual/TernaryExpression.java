package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 三元运算符表达式
 * 示例: condition ? trueValue : falseValue
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TernaryExpression extends Expression {

    /**
     * 条件表达式
     */
    private Expression condition;

    /**
     * true分支表达式
     */
    private Expression thenExpression;

    /**
     * false分支表达式
     */
    private Expression elseExpression;

    @Override
    public String toQL() {
        return "(" + condition.toQL() + " ? " + thenExpression.toQL() + " : " + elseExpression.toQL() + ")";
    }
}

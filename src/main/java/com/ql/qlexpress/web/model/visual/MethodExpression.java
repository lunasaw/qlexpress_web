package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 方法调用表达式
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class MethodExpression extends Expression {

    /**
     * 目标对象表达式
     */
    private Expression target;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 参数列表
     */
    private List<Expression> arguments = new ArrayList<>();

    @Override
    public String toQL() {
        String args = "";
        if (arguments != null && !arguments.isEmpty()) {
            args = arguments.stream()
                    .map(Expression::toQL)
                    .collect(Collectors.joining(", "));
        }
        return target.toQL() + "." + methodName + "(" + args + ")";
    }
}

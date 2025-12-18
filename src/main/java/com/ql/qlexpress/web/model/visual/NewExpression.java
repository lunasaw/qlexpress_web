package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 构造函数表达式 (new 表达式)
 * 示例: new HashMap(), new ArrayList(), new JSONObject()
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class NewExpression extends Expression {

    /**
     * 类名
     */
    private String className;

    /**
     * 构造参数列表
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
        return "new " + className + "(" + args + ")";
    }
}

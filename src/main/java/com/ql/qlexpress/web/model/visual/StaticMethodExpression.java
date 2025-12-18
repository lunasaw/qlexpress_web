package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 静态方法调用表达式
 * 示例: JSON.toJSONString(obj), LogUtils.infoRule("msg")
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class StaticMethodExpression extends Expression {

    /**
     * 类名
     */
    private String className;

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
        return className + "." + methodName + "(" + args + ")";
    }
}

package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 数组/列表访问表达式
 * 示例: arr[0], list[i], map["key"]
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ArrayAccessExpression extends Expression {

    /**
     * 数组/集合表达式
     */
    private Expression array;

    /**
     * 索引表达式
     */
    private Expression index;

    @Override
    public String toQL() {
        return array.toQL() + "[" + index.toQL() + "]";
    }
}

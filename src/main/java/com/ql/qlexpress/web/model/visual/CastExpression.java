package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 类型转换表达式
 * 示例: (String)obj, (AppBluetoothCommandModel)JSON.parseObject(str, class)
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class CastExpression extends Expression {

    /**
     * 目标类型
     */
    private String targetType;

    /**
     * 被转换的表达式
     */
    private Expression expression;

    @Override
    public String toQL() {
        return "(" + targetType + ")" + expression.toQL();
    }
}

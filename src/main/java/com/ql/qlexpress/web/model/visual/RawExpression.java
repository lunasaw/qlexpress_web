package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 原始代码表达式
 * 用于无法解析或需要原样输出的代码
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RawExpression extends Expression {

    /**
     * 原始代码
     */
    private String code;

    @Override
    public String toQL() {
        return code;
    }
}

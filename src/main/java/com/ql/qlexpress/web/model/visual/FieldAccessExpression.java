package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 字段访问表达式
 * 示例: obj.field, AppBluetoothCommandModel.class, AppBluetoothOperateTypeEnum.NOTIFY_APP_EXIST_AUTHED_DEVICE
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FieldAccessExpression extends Expression {

    /**
     * 目标对象表达式
     */
    private Expression object;

    /**
     * 字段名
     */
    private String field;

    @Override
    public String toQL() {
        return object.toQL() + "." + field;
    }
}

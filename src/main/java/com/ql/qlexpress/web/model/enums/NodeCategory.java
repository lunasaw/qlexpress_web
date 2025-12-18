package com.ql.qlexpress.web.model.enums;

/**
 * 节点分类枚举
 *
 * @author qlexpress
 */
public enum NodeCategory {
    /**
     * 控制流
     */
    CONTROL_FLOW,
    /**
     * 数据操作
     */
    DATA_OPERATION,
    /**
     * 函数调用
     */
    FUNCTION_CALL,
    /**
     * 表达式
     */
    EXPRESSION,
    /**
     * 变量
     */
    VARIABLE,
    /**
     * 输入输出
     */
    IO,
    /**
     * 业务组件
     */
    BUSINESS,
    /**
     * 自定义
     */
    CUSTOM
}

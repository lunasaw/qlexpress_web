package com.ql.qlexpress.web.model.enums;

/**
 * 追踪类型
 *
 * @author qlexpress
 */
public enum TraceType {
    /**
     * 变量赋值
     */
    ASSIGNMENT,

    /**
     * 表达式求值
     */
    EXPRESSION,

    /**
     * 函数调用
     */
    FUNCTION_CALL,

    /**
     * 条件判断
     */
    CONDITION,

    /**
     * 循环迭代
     */
    LOOP_ITERATION,

    /**
     * 返回语句
     */
    RETURN,

    /**
     * 异常
     */
    EXCEPTION,

    /**
     * 进入块
     */
    BLOCK_ENTER,

    /**
     * 退出块
     */
    BLOCK_EXIT
}

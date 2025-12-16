package com.ql.qlexpress.web.model.enums;

/**
 * 节点执行状态
 *
 * @author qlexpress
 */
public enum NodeExecutionStatus {
    /**
     * 等待执行
     */
    PENDING,

    /**
     * 正在执行
     */
    RUNNING,

    /**
     * 执行完成
     */
    COMPLETED,

    /**
     * 已跳过
     */
    SKIPPED,

    /**
     * 执行失败
     */
    FAILED
}

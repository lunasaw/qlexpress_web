package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Try-Catch节点数据
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TryCatchNodeData extends NodeData {
    /**
     * try块入口节点ID
     */
    private String tryBranch;

    /**
     * catch处理器列表
     */
    private List<CatchHandler> catchHandlers;

    /**
     * finally块入口节点ID（可选）
     */
    private String finallyBranch;

    /**
     * Catch处理器
     */
    @Data
    public static class CatchHandler {
        /**
         * 异常类型
         */
        private String exceptionType;

        /**
         * 异常变量名
         */
        private String exceptionVariable;

        /**
         * catch块入口节点ID
         */
        private String catchBranch;
    }
}

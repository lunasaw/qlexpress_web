package com.ql.qlexpress.web.liteflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 流程执行请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowExecuteRequest {

    /**
     * 执行参数
     */
    private Map<String, Object> params;

    /**
     * 执行选项
     */
    private ExecuteOptions options;

    /**
     * 执行选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteOptions {
        /**
         * 超时时间 (毫秒)
         */
        @Builder.Default
        private long timeout = 30000;

        /**
         * 是否启用追踪
         */
        @Builder.Default
        private boolean traceEnabled = true;

        /**
         * 是否自动部署 (如果未部署)
         */
        @Builder.Default
        private boolean autoDeploy = true;
    }
}

package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 解析QL脚本为可视化流程响应
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseToVisualResponse {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 解析得到的流程定义
     */
    private VisualFlowSchema flow;

    /**
     * 解析警告
     */
    private List<String> warnings;

    /**
     * 解析统计信息
     */
    private ParseStats stats;

    /**
     * 错误消息（如果失败）
     */
    private String errorMessage;

    /**
     * 创建成功响应
     */
    public static ParseToVisualResponse success(VisualFlowSchema flow, ParseStats stats) {
        return ParseToVisualResponse.builder()
                .success(true)
                .flow(flow)
                .stats(stats)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ParseToVisualResponse failure(String errorMessage) {
        return ParseToVisualResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 解析统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParseStats {
        /**
         * 解析耗时（毫秒）
         */
        private long parseTimeMs;

        /**
         * 节点数量
         */
        private int nodeCount;

        /**
         * 边数量
         */
        private int edgeCount;

        /**
         * 原始脚本行数
         */
        private int scriptLineCount;
    }
}

package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * 执行响应
 *
 * @author qlexpress
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteResponse {

    /**
     * 执行是否成功
     */
    private boolean success;

    /**
     * 执行结果
     */
    private Object result;

    /**
     * 结果类型
     */
    private String resultType;

    /**
     * 执行追踪信息
     */
    private List<TracePoint> traces;

    /**
     * 执行统计
     */
    private ExecuteStats stats;

    /**
     * 错误信息（如果失败）
     */
    private ExecuteError error;

    /**
     * 执行后的上下文变量
     */
    private Map<String, Object> outputContext;

    /**
     * 创建成功响应
     */
    public static ExecuteResponse success(Object result, ExecuteStats stats, Map<String, Object> outputContext) {
        return ExecuteResponse.builder()
                .success(true)
                .result(result)
                .resultType(result != null ? result.getClass().getSimpleName() : "null")
                .stats(stats)
                .outputContext(outputContext)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ExecuteResponse failure(ExecuteError error) {
        return ExecuteResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}

package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

/**
 * 执行流程响应
 *
 * @author qlexpress
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExecuteFlowResponse extends ExecuteResponse {

    /**
     * 节点执行详情
     */
    private List<NodeExecutionDetail> nodeDetails;

    /**
     * 执行路径（经过的节点ID列表）
     */
    private List<String> executionPath;

    /**
     * 生成的QL脚本（用于调试）
     */
    private String generatedScript;

    /**
     * 创建成功响应
     */
    public static ExecuteFlowResponse success(Object result, ExecuteStats stats,
                                              Map<String, Object> outputContext,
                                              List<NodeExecutionDetail> nodeDetails,
                                              List<String> executionPath,
                                              String generatedScript) {
        ExecuteFlowResponse response = ExecuteFlowResponse.builder()
                .success(true)
                .result(result)
                .resultType(result != null ? result.getClass().getSimpleName() : "null")
                .stats(stats)
                .outputContext(outputContext)
                .nodeDetails(nodeDetails)
                .executionPath(executionPath)
                .generatedScript(generatedScript)
                .build();
        return response;
    }
}

package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.NodeExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 节点执行详情
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeExecutionDetail {

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 执行状态
     */
    private NodeExecutionStatus status;

    /**
     * 进入时间（毫秒时间戳）
     */
    private long entryTime;

    /**
     * 退出时间（毫秒时间戳）
     */
    private long exitTime;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 输入值
     */
    private Map<String, Object> inputValues;

    /**
     * 输出值
     */
    private Map<String, Object> outputValues;

    /**
     * 错误信息
     */
    private String error;
}

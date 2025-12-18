package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 错误位置
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLocation {

    /**
     * 行号（从1开始）
     */
    private int line;

    /**
     * 列号（从1开始）
     */
    private int column;

    /**
     * 结束行号
     */
    private int endLine;

    /**
     * 结束列号
     */
    private int endColumn;

    /**
     * 节点ID（如果是可视化流程中的错误）
     */
    private String nodeId;
}

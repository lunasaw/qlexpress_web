package com.ql.qlexpress.web.core.transpiler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 源码映射（节点ID到代码行号的映射）
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceMapping {
    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 起始行号
     */
    private int startLine;

    /**
     * 结束行号
     */
    private int endLine;

    /**
     * 起始列号
     */
    private int startColumn;

    /**
     * 结束列号
     */
    private int endColumn;
}

package com.ql.qlexpress.web.core.transpiler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转译统计
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranspileStats {
    /**
     * 节点数量
     */
    private int nodeCount;

    /**
     * 连线数量
     */
    private int edgeCount;

    /**
     * 生成代码行数
     */
    private int lineCount;

    /**
     * 转译耗时（毫秒）
     */
    private long transpileTimeMs;
}

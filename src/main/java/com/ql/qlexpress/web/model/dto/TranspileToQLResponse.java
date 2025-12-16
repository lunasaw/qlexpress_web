package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.core.transpiler.SourceMapping;
import com.ql.qlexpress.web.core.transpiler.TranspileStats;
import com.ql.qlexpress.web.core.transpiler.TranspileWarning;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 可视化JSON转QL脚本响应
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranspileToQLResponse {

    /**
     * 生成的QL脚本
     */
    private String script;

    /**
     * 源映射（节点ID到代码行号的映射）
     */
    private Map<String, SourceMapping> sourceMap;

    /**
     * 转译警告
     */
    private List<TranspileWarning> warnings;

    /**
     * 转译统计
     */
    private TranspileStats stats;
}

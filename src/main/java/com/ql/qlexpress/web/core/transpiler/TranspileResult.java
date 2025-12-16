package com.ql.qlexpress.web.core.transpiler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 转译结果
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranspileResult {
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

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息（如果失败）
     */
    private String errorMessage;

    /**
     * 创建成功结果
     */
    public static TranspileResult success(String script, Map<String, SourceMapping> sourceMap,
                                          List<TranspileWarning> warnings, TranspileStats stats) {
        return TranspileResult.builder()
                .success(true)
                .script(script)
                .sourceMap(sourceMap)
                .warnings(warnings)
                .stats(stats)
                .build();
    }

    /**
     * 创建失败结果
     */
    public static TranspileResult failure(String errorMessage, List<TranspileWarning> warnings) {
        return TranspileResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .warnings(warnings)
                .build();
    }
}

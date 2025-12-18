package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.transpiler.TranspileOptions;
import com.ql.qlexpress.web.core.transpiler.TranspileResult;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.exception.TranspileException;
import com.ql.qlexpress.web.model.dto.TranspileToQLResponse;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 转译服务
 *
 * @author qlexpress
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranspileService {

    private final VisualToQLTranspiler visualToQLTranspiler;

    /**
     * 将可视化流程JSON转换为QL脚本
     *
     * @param flow    流程定义
     * @param options 转译选项（可选）
     * @return 转译响应
     */
    public TranspileToQLResponse transpileToQL(VisualFlowSchema flow, TranspileOptions options) {
        log.info("开始转译流程，节点数量: {}", flow.getNodes() != null ? flow.getNodes().size() : 0);

        // 使用默认选项（如果未提供）
        if (options == null) {
            options = new TranspileOptions();
        }

        // 执行转译
        TranspileResult result = visualToQLTranspiler.transpile(flow, options);

        // 检查结果
        if (!result.isSuccess()) {
            log.error("转译失败: {}", result.getErrorMessage());
            throw new TranspileException(result.getErrorMessage());
        }

        log.info("转译成功，生成脚本行数: {}, 耗时: {}ms",
                result.getStats().getLineCount(),
                result.getStats().getTranspileTimeMs());

        // 构建响应
        return TranspileToQLResponse.builder()
                .script(result.getScript())
                .sourceMap(result.getSourceMap())
                .warnings(result.getWarnings())
                .stats(result.getStats())
                .build();
    }

    /**
     * 将可视化流程JSON转换为QL脚本（使用默认选项）
     *
     * @param flow 流程定义
     * @return 转译响应
     */
    public TranspileToQLResponse transpileToQL(VisualFlowSchema flow) {
        return transpileToQL(flow, null);
    }

    /**
     * 快速转译（仅返回脚本字符串）
     *
     * @param flow 流程定义
     * @return QL脚本字符串
     */
    public String quickTranspile(VisualFlowSchema flow) {
        TranspileToQLResponse response = transpileToQL(flow);
        return response.getScript();
    }

    /**
     * 验证并转译（带格式化）
     *
     * @param flow 流程定义
     * @return 格式化的QL脚本
     */
    public String transpileWithFormat(VisualFlowSchema flow) {
        TranspileOptions options = TranspileOptions.builder()
                .format(true)
                .generateComments(true)
                .build();
        TranspileToQLResponse response = transpileToQL(flow, options);
        return response.getScript();
    }

    /**
     * 转译为紧凑格式
     *
     * @param flow 流程定义
     * @return 紧凑格式的QL脚本
     */
    public String transpileCompact(VisualFlowSchema flow) {
        TranspileOptions options = TranspileOptions.builder()
                .format(false)
                .build();
        TranspileToQLResponse response = transpileToQL(flow, options);
        return response.getScript();
    }
}

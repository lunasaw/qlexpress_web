package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.parser.QLToVisualParser;
import com.ql.qlexpress.web.model.dto.ParseToVisualRequest;
import com.ql.qlexpress.web.model.dto.ParseToVisualResponse;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 解析服务
 *
 * 提供QL脚本到可视化流程的解析能力
 *
 * @author qlexpress
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParseService {

    private final QLToVisualParser parser;

    /**
     * 将QL脚本解析为可视化流程
     *
     * @param request 解析请求
     * @return 解析响应
     */
    public ParseToVisualResponse parseToVisual(ParseToVisualRequest request) {
        log.info("开始解析QL脚本，长度: {} 字符", request.getScript().length());

        long startTime = System.currentTimeMillis();

        try {
            // 解析脚本
            QLToVisualParser.ParseResult result = parser.parse(request.getScript());

            long parseTime = System.currentTimeMillis() - startTime;

            if (!result.isSuccess()) {
                log.warn("QL脚本解析失败: {}", result.getErrorMessage());
                return ParseToVisualResponse.failure(result.getErrorMessage());
            }

            VisualFlowSchema flow = result.getFlow();

            // 计算统计信息
            ParseToVisualResponse.ParseStats stats = ParseToVisualResponse.ParseStats.builder()
                    .parseTimeMs(parseTime)
                    .nodeCount(flow.getNodes() != null ? flow.getNodes().size() : 0)
                    .edgeCount(flow.getEdges() != null ? flow.getEdges().size() : 0)
                    .scriptLineCount(countLines(request.getScript()))
                    .build();

            log.info("QL脚本解析成功，耗时: {}ms，节点数: {}，边数: {}",
                    parseTime, stats.getNodeCount(), stats.getEdgeCount());

            return ParseToVisualResponse.success(flow, stats);

        } catch (Exception e) {
            log.error("QL脚本解析异常: {}", e.getMessage(), e);
            return ParseToVisualResponse.failure("解析异常: " + e.getMessage());
        }
    }

    /**
     * 快速解析（仅返回流程定义）
     *
     * @param script QL脚本
     * @return 流程定义
     */
    public VisualFlowSchema quickParse(String script) {
        ParseToVisualRequest request = ParseToVisualRequest.builder()
                .script(script)
                .build();

        ParseToVisualResponse response = parseToVisual(request);

        if (!response.isSuccess()) {
            throw new RuntimeException(response.getErrorMessage());
        }

        return response.getFlow();
    }

    /**
     * 计算脚本行数
     */
    private int countLines(String script) {
        if (script == null || script.isEmpty()) {
            return 0;
        }
        return script.split("\n").length;
    }
}

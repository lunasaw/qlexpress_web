package com.ql.qlexpress.web.core.transpiler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 转译上下文
 *
 * @author qlexpress
 */
@Getter
public class TranspileContext {
    /**
     * 转译选项
     */
    private final TranspileOptions options;

    /**
     * 源映射
     */
    private final Map<String, SourceMapping> sourceMap;

    /**
     * 警告列表
     */
    private final List<TranspileWarning> warnings;

    /**
     * 当前行号
     */
    private int currentLine;

    /**
     * 当前缩进级别
     */
    private int indentLevel;

    /**
     * 开始时间
     */
    private final long startTime;

    public TranspileContext() {
        this(new TranspileOptions());
    }

    public TranspileContext(TranspileOptions options) {
        this.options = options != null ? options : new TranspileOptions();
        this.sourceMap = new HashMap<>();
        this.warnings = new ArrayList<>();
        this.currentLine = 1;
        this.indentLevel = 0;
        this.startTime = System.currentTimeMillis();
    }

    /**
     * 增加当前行号
     */
    public void incrementLine() {
        this.currentLine++;
    }

    /**
     * 增加当前行号
     */
    public void incrementLine(int count) {
        this.currentLine += count;
    }

    /**
     * 增加缩进级别
     */
    public void indent() {
        this.indentLevel++;
    }

    /**
     * 减少缩进级别
     */
    public void dedent() {
        if (this.indentLevel > 0) {
            this.indentLevel--;
        }
    }

    /**
     * 获取当前缩进字符串
     */
    public String getIndent() {
        if (!options.isFormat()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append(options.getIndent());
        }
        return sb.toString();
    }

    /**
     * 添加源映射
     */
    public void addSourceMapping(String nodeId, int startLine, int endLine) {
        sourceMap.put(nodeId, SourceMapping.builder()
                .nodeId(nodeId)
                .startLine(startLine)
                .endLine(endLine)
                .startColumn(0)
                .endColumn(0)
                .build());
    }

    /**
     * 添加源映射（带列号）
     */
    public void addSourceMapping(String nodeId, int startLine, int endLine, int startColumn, int endColumn) {
        sourceMap.put(nodeId, SourceMapping.builder()
                .nodeId(nodeId)
                .startLine(startLine)
                .endLine(endLine)
                .startColumn(startColumn)
                .endColumn(endColumn)
                .build());
    }

    /**
     * 添加警告
     */
    public void addWarning(String nodeId, String code, String message) {
        warnings.add(TranspileWarning.warning(nodeId, code, message));
    }

    /**
     * 添加错误
     */
    public void addError(String nodeId, String code, String message) {
        warnings.add(TranspileWarning.error(nodeId, code, message));
    }

    /**
     * 添加信息
     */
    public void addInfo(String nodeId, String code, String message) {
        warnings.add(TranspileWarning.info(nodeId, code, message));
    }

    /**
     * 获取转译耗时
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * 构建转译统计
     */
    public TranspileStats buildStats(int nodeCount, int edgeCount, int lineCount) {
        return TranspileStats.builder()
                .nodeCount(nodeCount)
                .edgeCount(edgeCount)
                .lineCount(lineCount)
                .transpileTimeMs(getElapsedTime())
                .build();
    }

    /**
     * 是否格式化输出
     */
    public boolean isFormat() {
        return options.isFormat();
    }

    /**
     * 是否生成注释
     */
    public boolean isGenerateComments() {
        return options.isGenerateComments();
    }
}

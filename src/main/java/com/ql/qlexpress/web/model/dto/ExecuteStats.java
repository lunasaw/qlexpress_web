package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行统计
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteStats {

    /**
     * 执行耗时（毫秒）
     */
    private long executionTimeMs;

    /**
     * 编译耗时（毫秒）
     */
    private long compileTimeMs;

    /**
     * 是否命中缓存
     */
    private boolean cacheHit;

    /**
     * 执行的指令数
     */
    private long instructionCount;

    /**
     * 函数调用次数
     */
    private long functionCallCount;

    /**
     * 循环执行次数
     */
    private long loopIterationCount;
}

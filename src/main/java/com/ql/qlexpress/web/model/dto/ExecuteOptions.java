package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 执行选项
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteOptions {

    /**
     * 超时时间（毫秒）
     */
    @Min(100)
    @Max(300000)
    @Builder.Default
    private long timeout = 30000;

    /**
     * 是否启用表达式追踪
     */
    @Builder.Default
    private boolean enableTrace = false;

    /**
     * 是否启用缓存
     */
    @Builder.Default
    private boolean enableCache = true;

    /**
     * 是否在沙箱中执行
     */
    @Builder.Default
    private boolean sandbox = true;

    /**
     * 最大循环次数
     */
    @Max(1000000)
    @Builder.Default
    private int maxLoopCount = 100000;

    /**
     * 是否精确的BigDecimal运算
     */
    @Builder.Default
    private boolean preciseBigDecimal = false;

    /**
     * 是否启用短路求值
     */
    @Builder.Default
    private boolean shortCircuit = true;
}

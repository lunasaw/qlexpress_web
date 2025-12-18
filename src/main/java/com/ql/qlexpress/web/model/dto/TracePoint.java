package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.TraceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 追踪点
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracePoint {

    /**
     * 追踪类型
     */
    private TraceType type;

    /**
     * 表达式Token
     */
    private String token;

    /**
     * 行号
     */
    private int line;

    /**
     * 列号
     */
    private int column;

    /**
     * 执行前的值
     */
    private Object valueBefore;

    /**
     * 执行后的值
     */
    private Object valueAfter;

    /**
     * 执行耗时（纳秒）
     */
    private long durationNanos;

    /**
     * 子追踪点
     */
    private List<TracePoint> children;
}

package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连线样式
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeStyle {
    /**
     * 线条颜色
     */
    private String stroke;

    /**
     * 线条宽度
     */
    private Integer strokeWidth;

    /**
     * 线条样式（solid, dashed, dotted）
     */
    private String strokeDasharray;
}

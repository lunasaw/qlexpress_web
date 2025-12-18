package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点样式
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeStyle {
    /**
     * 背景颜色
     */
    private String backgroundColor;

    /**
     * 边框颜色
     */
    private String borderColor;

    /**
     * 边框宽度
     */
    private Integer borderWidth;

    /**
     * 边框圆角
     */
    private Integer borderRadius;

    /**
     * 宽度
     */
    private Integer width;

    /**
     * 高度
     */
    private Integer height;
}

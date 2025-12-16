package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连接点模板
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandleTemplate {
    /**
     * 连接点ID
     */
    private String id;

    /**
     * 类型（source/target）
     */
    private String type;

    /**
     * 位置（top/bottom/left/right）
     */
    private String position;

    /**
     * 标签
     */
    private String label;

    /**
     * 期望的数据类型
     */
    private String dataType;

    /**
     * 是否必须连接
     */
    private boolean required;

    /**
     * 最大连接数（-1表示无限）
     */
    private int maxConnections;
}

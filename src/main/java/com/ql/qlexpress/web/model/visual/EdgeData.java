package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连线数据
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeData {
    /**
     * 条件标签（用于条件分支）
     */
    private String conditionLabel;

    /**
     * 条件值（用于条件分支，如 true/false）
     */
    private String conditionValue;
}

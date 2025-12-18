package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选项项
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionItem {
    /**
     * 选项值
     */
    private Object value;

    /**
     * 显示文本
     */
    private String label;

    /**
     * 描述
     */
    private String description;
}

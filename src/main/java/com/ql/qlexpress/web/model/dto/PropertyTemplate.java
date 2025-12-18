package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.PropertyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 属性模板
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyTemplate {
    /**
     * 属性名
     */
    private String name;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 属性类型
     */
    private PropertyType type;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 验证规则
     */
    private String validation;

    /**
     * 可选值（用于select类型）
     */
    private List<OptionItem> options;

    /**
     * 分组
     */
    private String group;

    /**
     * 帮助文本
     */
    private String helpText;
}

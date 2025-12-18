package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 函数参数元数据
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterMetadata {
    /**
     * 参数名
     */
    private String name;

    /**
     * 参数类型
     */
    private String type;

    /**
     * 是否必填
     */
    private boolean required;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 描述
     */
    private String description;

    /**
     * 可选值列表（枚举类型）
     */
    private List<Object> options;
}

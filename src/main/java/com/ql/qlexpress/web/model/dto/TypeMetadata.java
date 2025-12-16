package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 类型元数据
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeMetadata {
    /**
     * 类型名称
     */
    private String name;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否为基础类型
     */
    private boolean primitive;

    /**
     * 可用方法
     */
    private List<String> methods;

    /**
     * 可转换的类型
     */
    private List<String> convertibleTo;
}

package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 元数据响应
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataResponse {
    /**
     * 元数据版本
     */
    private String version;

    /**
     * 操作符列表
     */
    private List<OperatorMetadata> operators;

    /**
     * 内置函数列表
     */
    private List<FunctionMetadata> builtInFunctions;

    /**
     * 自定义函数列表
     */
    private List<FunctionMetadata> customFunctions;

    /**
     * 关键字列表
     */
    private List<KeywordMetadata> keywords;

    /**
     * 类型列表
     */
    private List<TypeMetadata> types;

    /**
     * 节点模板列表
     */
    private List<NodeTemplate> nodeTemplates;

    /**
     * 分类信息
     */
    private List<CategoryInfo> categories;
}

package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.FunctionSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 函数元数据
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionMetadata {
    /**
     * 函数名称列表（包含别名）
     */
    private List<String> names;

    /**
     * 主名称
     */
    private String primaryName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * 参数列表
     */
    private List<ParameterMetadata> parameters;

    /**
     * 是否支持可变参数
     */
    private boolean varargs;

    /**
     * 使用示例
     */
    private List<FunctionExample> examples;

    /**
     * 函数来源
     */
    private FunctionSource source;

    /**
     * 是否已废弃
     */
    private boolean deprecated;

    /**
     * 废弃说明
     */
    private String deprecatedMessage;
}

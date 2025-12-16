package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 关键字元数据
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeywordMetadata {
    /**
     * 关键字
     */
    private String keyword;

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
     * 使用示例
     */
    private List<String> examples;
}

package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.NodeCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 节点模板
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeTemplate {
    /**
     * 节点类型
     */
    private String type;

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
    private NodeCategory category;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 默认宽度
     */
    private int defaultWidth;

    /**
     * 默认高度
     */
    private int defaultHeight;

    /**
     * 输入连接点
     */
    private List<HandleTemplate> inputHandles;

    /**
     * 输出连接点
     */
    private List<HandleTemplate> outputHandles;

    /**
     * 属性模板
     */
    private List<PropertyTemplate> properties;

    /**
     * 默认数据
     */
    private Map<String, Object> defaultData;

    /**
     * 是否可删除
     */
    private boolean deletable;

    /**
     * 是否可复制
     */
    private boolean copyable;

    /**
     * 验证规则
     */
    private List<ValidationRule> validationRules;
}

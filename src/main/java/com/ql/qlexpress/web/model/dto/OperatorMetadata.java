package com.ql.qlexpress.web.model.dto;

import com.ql.qlexpress.web.model.enums.OperatorCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 操作符元数据
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorMetadata {
    /**
     * 操作符符号
     */
    private String symbol;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 描述
     */
    private String description;

    /**
     * 优先级
     */
    private int precedence;

    /**
     * 分类
     */
    private OperatorCategory category;

    /**
     * 左操作数类型
     */
    private String leftType;

    /**
     * 右操作数类型
     */
    private String rightType;

    /**
     * 结果类型
     */
    private String resultType;

    /**
     * 是否为一元操作符
     */
    private boolean unary;

    /**
     * 使用示例
     */
    private List<String> examples;

    /**
     * 是否为自定义操作符
     */
    private boolean custom;
}

package com.ql.qlexpress.web.liteflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可视化组件树结构 (参考 liteflow-editor-server 核心模型)
 * 用于前端可视化画布与后端 EL 表达式之间的双向转换
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CmpProperty {

    /**
     * 组件ID (叶子节点为组件引用，编排节点为自动生成的ID)
     */
    private String id;

    /**
     * 组件类型：
     * - 编排类型: THEN, WHEN, IF, SWITCH, FOR, WHILE, CATCH, AND, OR, NOT
     * - 节点类型: CommonNode, BooleanNode, SwitchNode (对应 QLComponent)
     */
    private String type;

    /**
     * 组件引用 (对于叶子节点，引用 QLComponent 的 componentId)
     */
    private String componentRef;

    /**
     * 组件属性
     */
    private Properties properties;

    /**
     * 条件部分 (IF/SWITCH/FOR/WHILE 等需要)
     */
    private CmpProperty condition;

    /**
     * 子节点列表 (组成树结构)
     */
    private List<CmpProperty> children;

    /**
     * 属性详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Properties {
        /**
         * 组件标识 (用于 .id("xxx") 修饰符)
         */
        private String id;

        /**
         * 组件标签 (用于 .tag("xxx") 修饰符)
         */
        private String tag;

        /**
         * LiteFlow 参数 (用于 .data("xxx") 修饰符)
         */
        private String data;

        /**
         * 分支标识 (用于 SWITCH 的 .to() 映射)
         */
        private String branch;

        /**
         * 最大等待时间 (用于 WHEN 的 .maxWaitSeconds())
         */
        private Integer maxWaitSeconds;

        /**
         * 是否忽略错误 (用于 .ignoreError(true))
         */
        private Boolean ignoreError;

        /**
         * 是否必须成功 (用于 WHEN 的 .must())
         */
        private Boolean must;

        /**
         * 循环次数 (用于 FOR)
         */
        private Integer loopCount;

        /**
         * 中断条件 (用于 WHILE 的 BREAK)
         */
        private CmpProperty breakCondition;
    }

    /**
     * 编排类型枚举
     */
    public enum CmpType {
        // 编排类型
        THEN,           // 顺序执行
        WHEN,           // 并行执行
        IF,             // 条件分支
        SWITCH,         // 多路分支
        FOR,            // 循环
        WHILE,          // 条件循环
        CATCH,          // 异常捕获
        AND,            // 逻辑与
        OR,             // 逻辑或
        NOT,            // 逻辑非

        // 节点类型 (对应 QLComponent)
        CommonNode,     // 普通脚本节点 (SCRIPT)
        BooleanNode,    // 布尔判断节点 (BOOLEAN)
        SwitchNode,     // 路由选择节点 (SWITCH)

        // 特殊类型
        BREAK,          // 循环中断
        COMPONENT       // 组件引用
    }

    /**
     * 判断是否是编排类型
     */
    public boolean isOrchestration() {
        if (type == null) return false;
        try {
            CmpType cmpType = CmpType.valueOf(type.toUpperCase());
            return cmpType == CmpType.THEN || cmpType == CmpType.WHEN ||
                    cmpType == CmpType.IF || cmpType == CmpType.SWITCH ||
                    cmpType == CmpType.FOR || cmpType == CmpType.WHILE ||
                    cmpType == CmpType.CATCH || cmpType == CmpType.AND ||
                    cmpType == CmpType.OR || cmpType == CmpType.NOT;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 判断是否是叶子节点 (组件引用)
     */
    public boolean isLeafNode() {
        return !isOrchestration() && (componentRef != null || id != null);
    }

    /**
     * 获取实际的节点ID (用于EL表达式生成)
     */
    public String getNodeId() {
        return componentRef != null ? componentRef : id;
    }
}

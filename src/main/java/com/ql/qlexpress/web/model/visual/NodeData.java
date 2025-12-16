package com.ql.qlexpress.web.model.visual;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

/**
 * 节点数据基类
 *
 * @author qlexpress
 */
@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "nodeType", defaultImpl = GenericNodeData.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StartNodeData.class, name = "start"),
        @JsonSubTypes.Type(value = EndNodeData.class, name = "end"),
        @JsonSubTypes.Type(value = IfNodeData.class, name = "if"),
        @JsonSubTypes.Type(value = ForNodeData.class, name = "for"),
        @JsonSubTypes.Type(value = ForeachNodeData.class, name = "foreach"),
        @JsonSubTypes.Type(value = WhileNodeData.class, name = "while"),
        @JsonSubTypes.Type(value = ExpressionNodeData.class, name = "expression"),
        @JsonSubTypes.Type(value = AssignmentNodeData.class, name = "assignment"),
        @JsonSubTypes.Type(value = FunctionCallNodeData.class, name = "function_call"),
        @JsonSubTypes.Type(value = ReturnNodeData.class, name = "return"),
        @JsonSubTypes.Type(value = BreakNodeData.class, name = "break"),
        @JsonSubTypes.Type(value = ContinueNodeData.class, name = "continue"),
        @JsonSubTypes.Type(value = TryCatchNodeData.class, name = "try_catch"),
        @JsonSubTypes.Type(value = ThrowNodeData.class, name = "throw"),
})
public abstract class NodeData {
    /**
     * 节点标签
     */
    private String label;
}

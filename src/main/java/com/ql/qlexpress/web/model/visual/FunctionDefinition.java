package com.ql.qlexpress.web.model.visual;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数定义
 * 包含函数签名和完整的函数体流程图
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDefinition {
    /**
     * 函数名
     */
    private String name;

    /**
     * 参数列表
     */
    @Builder.Default
    private List<ParameterDefinition> parameters = new ArrayList<>();

    /**
     * 返回类型
     */
    private String returnType;

    /**
     * 函数体入口节点ID (向后兼容)
     * @deprecated 使用 body.entranceNodeId 代替
     */
    @Deprecated
    private String bodyEntrance;

    /**
     * 函数体结构
     * 包含完整的节点和边，形成独立的子流程图
     */
    private FunctionBody body;

    /**
     * 描述
     */
    private String description;

    /**
     * 访问修饰符 (public, private, protected)
     */
    private String modifier;

    /**
     * 获取函数体入口节点ID
     * 优先使用新的body结构，向后兼容bodyEntrance
     */
    @JsonIgnore
    public String getEffectiveEntrance() {
        if (body != null && body.getEntranceNodeId() != null) {
            return body.getEntranceNodeId();
        }
        return bodyEntrance;
    }

    /**
     * 检查函数是否有函数体
     */
    @JsonIgnore
    public boolean hasBody() {
        return body != null && body.getNodes() != null && !body.getNodes().isEmpty();
    }

    /**
     * 生成函数签名的QL代码
     */
    public String toSignatureQL() {
        StringBuilder sb = new StringBuilder();
        if (modifier != null && !modifier.isEmpty()) {
            sb.append(modifier).append(" ");
        }
        if (returnType != null && !returnType.isEmpty()) {
            sb.append(returnType).append(" ");
        }
        sb.append(name).append("(");
        if (parameters != null && !parameters.isEmpty()) {
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                ParameterDefinition param = parameters.get(i);
                if (param.getType() != null && !param.getType().isEmpty()) {
                    sb.append(param.getType()).append(" ");
                }
                sb.append(param.getName());
            }
        }
        sb.append(")");
        return sb.toString();
    }
}

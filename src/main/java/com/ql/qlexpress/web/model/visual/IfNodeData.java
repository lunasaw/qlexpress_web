package com.ql.qlexpress.web.model.visual;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * IF条件节点数据
 * 支持完整的 if - else if - else 结构
 *
 * @author qlexpress
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IfNodeData extends NodeData {
    /**
     * 条件表达式
     */
    private Expression condition;

    /**
     * true分支入口节点ID
     */
    private String thenBranch;

    /**
     * else-if 分支列表
     * 按顺序存储所有 else if 条件及其对应的分支入口节点
     */
    private List<ElseIfBranch> elseIfBranches = new ArrayList<>();

    /**
     * false分支入口节点ID (最终的 else 分支)
     */
    private String elseBranch;

    /**
     * 添加 else-if 分支
     */
    public void addElseIfBranch(Expression condition, String branchNodeId) {
        if (elseIfBranches == null) {
            elseIfBranches = new ArrayList<>();
        }
        elseIfBranches.add(new ElseIfBranch(condition, branchNodeId));
    }

    /**
     * 检查是否有 else-if 分支
     */
    public boolean hasElseIfBranches() {
        return elseIfBranches != null && !elseIfBranches.isEmpty();
    }
}

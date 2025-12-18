package com.ql.qlexpress.web.model.visual;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * else-if 分支数据
 * 用于表示 if-else if-else 结构中的 else if 分支
 *
 * @author qlexpress
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElseIfBranch {

    /**
     * else-if 条件表达式
     */
    private Expression condition;

    /**
     * else-if 分支入口节点ID
     */
    private String branchNodeId;
}

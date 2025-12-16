package com.ql.qlexpress.web.core.transpiler;

import com.ql.qlexpress.web.model.enums.WarningSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转译警告
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranspileWarning {
    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 警告代码
     */
    private String code;

    /**
     * 警告消息
     */
    private String message;

    /**
     * 严重级别
     */
    private WarningSeverity severity;

    /**
     * 创建INFO级别警告
     */
    public static TranspileWarning info(String nodeId, String code, String message) {
        return TranspileWarning.builder()
                .nodeId(nodeId)
                .code(code)
                .message(message)
                .severity(WarningSeverity.INFO)
                .build();
    }

    /**
     * 创建WARNING级别警告
     */
    public static TranspileWarning warning(String nodeId, String code, String message) {
        return TranspileWarning.builder()
                .nodeId(nodeId)
                .code(code)
                .message(message)
                .severity(WarningSeverity.WARNING)
                .build();
    }

    /**
     * 创建ERROR级别警告
     */
    public static TranspileWarning error(String nodeId, String code, String message) {
        return TranspileWarning.builder()
                .nodeId(nodeId)
                .code(code)
                .message(message)
                .severity(WarningSeverity.ERROR)
                .build();
    }
}

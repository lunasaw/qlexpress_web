package com.ql.qlexpress.web.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 执行错误
 *
 * @author qlexpress
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteError {

    /**
     * 错误类型
     */
    private String type;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 错误位置
     */
    private ErrorLocation location;

    /**
     * 堆栈跟踪
     */
    private List<String> stackTrace;

    /**
     * 建议
     */
    private String suggestion;

    /**
     * 创建语法错误
     */
    public static ExecuteError syntaxError(String message, int line, int column) {
        return ExecuteError.builder()
                .type("SYNTAX_ERROR")
                .message(message)
                .location(ErrorLocation.builder().line(line).column(column).build())
                .suggestion("请检查脚本语法是否正确")
                .build();
    }

    /**
     * 创建运行时错误
     */
    public static ExecuteError runtimeError(String message, List<String> stackTrace) {
        return ExecuteError.builder()
                .type("RUNTIME_ERROR")
                .message(message)
                .stackTrace(stackTrace)
                .build();
    }

    /**
     * 创建超时错误
     */
    public static ExecuteError timeoutError(long timeout) {
        return ExecuteError.builder()
                .type("TIMEOUT_ERROR")
                .message("脚本执行超时，超过 " + timeout + " 毫秒")
                .suggestion("请优化脚本或增加超时时间")
                .build();
    }

    /**
     * 创建安全错误
     */
    public static ExecuteError securityError(String message) {
        return ExecuteError.builder()
                .type("SECURITY_ERROR")
                .message(message)
                .suggestion("脚本中包含不安全的操作")
                .build();
    }
}

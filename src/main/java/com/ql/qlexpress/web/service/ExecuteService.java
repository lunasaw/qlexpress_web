package com.ql.qlexpress.web.service;

import com.ql.qlexpress.web.core.transpiler.TranspileResult;
import com.ql.qlexpress.web.core.transpiler.VisualToQLTranspiler;
import com.ql.qlexpress.web.exception.BusinessException;
import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.model.enums.NodeExecutionStatus;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import com.ql.qlexpress.web.model.visual.VisualNode;
import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.InitOptions;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.exception.QLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * 执行服务
 *
 * @author qlexpress
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecuteService {

    private final VisualToQLTranspiler transpiler;
    private final TranspileService transpileService;

    /**
     * 执行QL脚本
     */
    public ExecuteResponse executeScript(ExecuteScriptRequest request) {
        log.info("执行QL脚本: {} 字符", request.getScript().length());

        ExecuteOptions options = request.getOptions() != null
                ? request.getOptions()
                : ExecuteOptions.builder().build();

        long startTime = System.currentTimeMillis();

        try {
            // 创建QLExpress运行器
            Express4Runner runner = createRunner(options);
            QLOptions qlOptions = createQLOptions(options);

            // 准备上下文
            Map<String, Object> context = request.getContext() != null
                    ? new HashMap<>(request.getContext())
                    : new HashMap<>();

            // 执行脚本（带超时控制）
            Object result = executeWithTimeout(runner, request.getScript(), context, qlOptions, options.getTimeout());

            long executionTime = System.currentTimeMillis() - startTime;

            // 构建统计信息
            ExecuteStats stats = ExecuteStats.builder()
                    .executionTimeMs(executionTime)
                    .compileTimeMs(0) // TODO: 从runner获取编译时间
                    .cacheHit(options.isEnableCache())
                    .build();

            log.info("脚本执行成功，耗时: {}ms, 结果类型: {}",
                    executionTime, result != null ? result.getClass().getSimpleName() : "null");

            return ExecuteResponse.success(result, stats, context);

        } catch (TimeoutException e) {
            log.error("脚本执行超时: {}ms", options.getTimeout());
            return ExecuteResponse.failure(ExecuteError.timeoutError(options.getTimeout()));

        } catch (QLException e) {
            log.error("脚本执行错误: {}", e.getMessage(), e);
            return ExecuteResponse.failure(createQLError(e));

        } catch (Exception e) {
            log.error("脚本执行异常: {}", e.getMessage(), e);
            return ExecuteResponse.failure(ExecuteError.builder()
                    .type("EXECUTION_ERROR")
                    .message(e.getMessage())
                    .stackTrace(extractStackTrace(e))
                    .build());
        }
    }

    /**
     * 执行可视化流程
     */
    public ExecuteFlowResponse executeFlow(ExecuteFlowRequest request) {
        log.info("执行可视化流程，节点数: {}", request.getFlow().getNodes().size());

        long startTime = System.currentTimeMillis();

        // 先转译为QL脚本
        String script;
        try {
            TranspileToQLResponse transpileResponse = transpileService.transpileToQL(request.getFlow());
            script = transpileResponse.getScript();
        } catch (Exception e) {
            return ExecuteFlowResponse.builder()
                    .success(false)
                    .error(ExecuteError.builder()
                            .type("TRANSPILE_ERROR")
                            .message(e.getMessage())
                            .build())
                    .build();
        }
        log.debug("生成的QL脚本:\n{}", script);

        ExecuteOptions options = request.getOptions() != null
                ? request.getOptions()
                : ExecuteOptions.builder().build();

        try {
            // 创建QLExpress运行器
            Express4Runner runner = createRunner(options);
            QLOptions qlOptions = createQLOptions(options);

            // 准备上下文
            Map<String, Object> context = request.getContext() != null
                    ? new HashMap<>(request.getContext())
                    : new HashMap<>();

            // 执行脚本
            Object result = executeWithTimeout(runner, script, context, qlOptions, options.getTimeout());

            long executionTime = System.currentTimeMillis() - startTime;

            // 构建统计信息
            ExecuteStats stats = ExecuteStats.builder()
                    .executionTimeMs(executionTime)
                    .compileTimeMs(0) // transpileTimeMs is tracked separately
                    .build();

            // 构建节点执行详情（简化版）
            List<NodeExecutionDetail> nodeDetails = null;
            List<String> executionPath = null;

            if (request.isIncludeNodeDetails()) {
                nodeDetails = buildNodeDetails(request.getFlow());
                executionPath = buildExecutionPath(request.getFlow());
            }

            log.info("流程执行成功，耗时: {}ms", executionTime);

            return ExecuteFlowResponse.success(
                    result, stats, context,
                    nodeDetails, executionPath, script
            );

        } catch (TimeoutException e) {
            log.error("流程执行超时: {}ms", options.getTimeout());
            return ExecuteFlowResponse.builder()
                    .success(false)
                    .error(ExecuteError.timeoutError(options.getTimeout()))
                    .generatedScript(script)
                    .build();

        } catch (QLException e) {
            log.error("流程执行错误: {}", e.getMessage(), e);
            return ExecuteFlowResponse.builder()
                    .success(false)
                    .error(createQLError(e))
                    .generatedScript(script)
                    .build();

        } catch (Exception e) {
            log.error("流程执行异常: {}", e.getMessage(), e);
            return ExecuteFlowResponse.builder()
                    .success(false)
                    .error(ExecuteError.builder()
                            .type("EXECUTION_ERROR")
                            .message(e.getMessage())
                            .stackTrace(extractStackTrace(e))
                            .build())
                    .generatedScript(script)
                    .build();
        }
    }

    /**
     * 创建QLExpress运行器
     */
    private Express4Runner createRunner(ExecuteOptions options) {
        InitOptions.Builder builder = InitOptions.builder();
        // 配置可以根据options进行调整
        // builder.debug(options.isDebug());

        return new Express4Runner(builder.build());
    }

    /**
     * 创建执行选项
     */
    private QLOptions createQLOptions(ExecuteOptions options) {
        QLOptions.Builder builder = QLOptions.builder();
        // 配置可以根据options进行调整
        // builder.maxLoopCount(options.getMaxLoopCount());
        // builder.preciseBigDecimal(options.isPreciseBigDecimal());
        // builder.shortCircuit(options.isShortCircuit());

        return builder.build();
    }

    /**
     * 带超时控制的执行
     */
    private Object executeWithTimeout(Express4Runner runner, String script,
                                      Map<String, Object> context, QLOptions qlOptions, long timeoutMs)
            throws TimeoutException, Exception {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<QLResult> future = executor.submit(() -> runner.execute(script, context, qlOptions));

        try {
            QLResult qlResult = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return qlResult.getResult();
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof QLException) {
                throw (QLException) cause;
            } else if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * 创建QL异常错误
     */
    private ExecuteError createQLError(QLException e) {
        ErrorLocation location = null;
        // TODO: 从QLException中提取位置信息
        // if (e.getLine() > 0) {
        //     location = ErrorLocation.builder()
        //             .line(e.getLine())
        //             .column(e.getColumn())
        //             .build();
        // }

        return ExecuteError.builder()
                .type(e.getClass().getSimpleName())
                .message(e.getMessage())
                .location(location)
                .stackTrace(extractStackTrace(e))
                .build();
    }

    /**
     * 提取堆栈跟踪
     */
    private List<String> extractStackTrace(Throwable e) {
        List<String> traces = new ArrayList<>();
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().startsWith("com.ql")) {
                traces.add(element.toString());
            }
            if (traces.size() >= 10) {
                break;
            }
        }
        return traces;
    }

    /**
     * 构建节点执行详情（简化版，因为我们没有实际的节点级追踪）
     */
    private List<NodeExecutionDetail> buildNodeDetails(VisualFlowSchema flow) {
        List<NodeExecutionDetail> details = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (VisualNode node : flow.getNodes()) {
            NodeExecutionStatus status = NodeExecutionStatus.COMPLETED;
            if ("start".equals(node.getType()) || "end".equals(node.getType())) {
                status = NodeExecutionStatus.COMPLETED;
            }

            details.add(NodeExecutionDetail.builder()
                    .nodeId(node.getId())
                    .nodeType(node.getType())
                    .status(status)
                    .entryTime(currentTime)
                    .exitTime(currentTime)
                    .durationMs(0)
                    .build());
        }

        return details;
    }

    /**
     * 构建执行路径（简化版）
     */
    private List<String> buildExecutionPath(VisualFlowSchema flow) {
        List<String> path = new ArrayList<>();
        for (VisualNode node : flow.getNodes()) {
            path.add(node.getId());
        }
        return path;
    }
}

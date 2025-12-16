package com.ql.qlexpress.web.controller;

import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.service.ExecuteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 执行控制器
 *
 * 提供QL脚本和可视化流程的执行能力
 *
 * @author qlexpress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
@Validated
public class ExecuteController {

    private final ExecuteService executeService;

    /**
     * 执行QL脚本
     *
     * POST /api/v1/execute/script
     *
     * @param request 执行请求
     * @return 执行结果
     */
    @PostMapping("/script")
    public ResponseEntity<ApiResponse<ExecuteResponse>> executeScript(
            @Valid @RequestBody ExecuteScriptRequest request) {
        log.info("接收到脚本执行请求");

        ExecuteResponse result = executeService.executeScript(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.ok(ApiResponse.<ExecuteResponse>builder()
                    .success(false)
                    .code("EXECUTION_FAILED")
                    .message(result.getError() != null ? result.getError().getMessage() : "执行失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 执行可视化流程
     *
     * POST /api/v1/execute/flow
     *
     * @param request 执行请求
     * @return 执行结果
     */
    @PostMapping("/flow")
    public ResponseEntity<ApiResponse<ExecuteFlowResponse>> executeFlow(
            @Valid @RequestBody ExecuteFlowRequest request) {
        log.info("接收到流程执行请求");

        ExecuteFlowResponse result = executeService.executeFlow(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.ok(ApiResponse.<ExecuteFlowResponse>builder()
                    .success(false)
                    .code("EXECUTION_FAILED")
                    .message(result.getError() != null ? result.getError().getMessage() : "执行失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 快速执行脚本（简化参数）
     *
     * POST /api/v1/execute/quick
     *
     * @param script 脚本内容
     * @return 执行结果
     */
    @PostMapping("/quick")
    public ResponseEntity<ApiResponse<Object>> quickExecute(@RequestBody String script) {
        log.info("接收到快速执行请求");

        ExecuteScriptRequest request = ExecuteScriptRequest.builder()
                .script(script)
                .options(ExecuteOptions.builder()
                        .timeout(10000) // 10秒超时
                        .sandbox(true)
                        .build())
                .build();

        ExecuteResponse result = executeService.executeScript(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result.getResult()));
        } else {
            return ResponseEntity.ok(ApiResponse.<Object>builder()
                    .success(false)
                    .code("EXECUTION_FAILED")
                    .message(result.getError() != null ? result.getError().getMessage() : "执行失败")
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 执行脚本并返回详���结果（包含追踪信息）
     *
     * POST /api/v1/execute/trace
     *
     * @param request 执行请求
     * @return 执行结果（带追踪）
     */
    @PostMapping("/trace")
    public ResponseEntity<ApiResponse<ExecuteResponse>> executeWithTrace(
            @Valid @RequestBody ExecuteScriptRequest request) {
        log.info("接收到带追踪的执行请求");

        // 强制启用追踪
        if (request.getOptions() == null) {
            request.setOptions(ExecuteOptions.builder().build());
        }
        request.getOptions().setEnableTrace(true);

        ExecuteResponse result = executeService.executeScript(request);

        return ResponseEntity.ok(result.isSuccess()
                ? ApiResponse.success(result)
                : ApiResponse.<ExecuteResponse>builder()
                        .success(false)
                        .code("EXECUTION_FAILED")
                        .message(result.getError() != null ? result.getError().getMessage() : "执行失败")
                        .data(result)
                        .timestamp(System.currentTimeMillis())
                        .build());
    }
}

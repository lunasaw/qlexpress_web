package com.ql.qlexpress.web.controller;

import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.service.ValidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 验证控制器
 *
 * 提供脚本和流程的验证能力
 *
 * @author qlexpress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validate")
@RequiredArgsConstructor
@Validated
public class ValidateController {

    private final ValidateService validateService;

    /**
     * 验证QL脚本语法
     *
     * POST /api/v1/validate/script
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/script")
    public ResponseEntity<ApiResponse<ValidateScriptResponse>> validateScript(
            @Valid @RequestBody ValidateScriptRequest request) {
        log.info("接收到脚本验证请求");

        ValidateScriptResponse result = validateService.validateScript(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 验证可视化流程
     *
     * POST /api/v1/validate/flow
     *
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/flow")
    public ResponseEntity<ApiResponse<ValidateFlowResponse>> validateFlow(
            @Valid @RequestBody ValidateFlowRequest request) {
        log.info("接收到流程验证请求");

        ValidateFlowResponse result = validateService.validateFlow(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 快速验证脚本（仅语法检查）
     *
     * POST /api/v1/validate/quick
     *
     * @param script 脚本内容
     * @return 是否有效
     */
    @PostMapping("/quick")
    public ResponseEntity<ApiResponse<Boolean>> quickValidate(@RequestBody String script) {
        log.info("接收到快速验证请求");

        ValidateScriptRequest request = ValidateScriptRequest.builder()
                .script(script)
                .checkSemantics(false)
                .checkBestPractices(false)
                .build();

        ValidateScriptResponse result = validateService.validateScript(request);

        return ResponseEntity.ok(ApiResponse.success(result.isValid()));
    }

    /**
     * 分析脚本复杂度
     *
     * POST /api/v1/validate/complexity
     *
     * @param script 脚本内容
     * @return 复杂度分析结果
     */
    @PostMapping("/complexity")
    public ResponseEntity<ApiResponse<ComplexityAnalysis>> analyzeComplexity(@RequestBody String script) {
        log.info("接收到复杂度分析请求");

        ValidateScriptRequest request = ValidateScriptRequest.builder()
                .script(script)
                .checkSemantics(false)
                .checkBestPractices(false)
                .build();

        ValidateScriptResponse result = validateService.validateScript(request);

        if (result.getComplexity() != null) {
            return ResponseEntity.ok(ApiResponse.success(result.getComplexity()));
        } else {
            return ResponseEntity.ok(ApiResponse.<ComplexityAnalysis>builder()
                    .success(false)
                    .code("ANALYSIS_FAILED")
                    .message("无法分析脚本复杂度")
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 完整验证脚本（语法+语义+最佳实践）
     *
     * POST /api/v1/validate/full
     *
     * @param script 脚本内容
     * @return 完整验证结果
     */
    @PostMapping("/full")
    public ResponseEntity<ApiResponse<ValidateScriptResponse>> fullValidate(@RequestBody String script) {
        log.info("接收到完整验证请求");

        ValidateScriptRequest request = ValidateScriptRequest.builder()
                .script(script)
                .checkSemantics(true)
                .checkBestPractices(true)
                .build();

        ValidateScriptResponse result = validateService.validateScript(request);

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}

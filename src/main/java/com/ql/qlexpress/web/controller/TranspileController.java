package com.ql.qlexpress.web.controller;

import com.ql.qlexpress.web.model.dto.ApiResponse;
import com.ql.qlexpress.web.model.dto.TranspileToQLRequest;
import com.ql.qlexpress.web.model.dto.TranspileToQLResponse;
import com.ql.qlexpress.web.service.TranspileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 转译控制器
 *
 * @author qlexpress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transpile")
@RequiredArgsConstructor
public class TranspileController {

    private final TranspileService transpileService;

    /**
     * 将可视化流程JSON转换为QL脚本
     *
     * POST /api/v1/transpile/to-ql
     *
     * @param request 转译请求
     * @return 转译响应
     */
    @PostMapping("/to-ql")
    public ResponseEntity<ApiResponse<TranspileToQLResponse>> transpileToQL(
            @Valid @RequestBody TranspileToQLRequest request) {

        log.info("收到转译请求，流程版本: {}",
                request.getFlow() != null && request.getFlow().getVersion() != null
                        ? request.getFlow().getVersion() : "未知");

        TranspileToQLResponse response = transpileService.transpileToQL(
                request.getFlow(),
                request.getOptions()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 快速转译（仅返回脚本）
     *
     * POST /api/v1/transpile/quick
     *
     * @param request 转译请求
     * @return 脚本字符串
     */
    @PostMapping("/quick")
    public ResponseEntity<ApiResponse<String>> quickTranspile(
            @Valid @RequestBody TranspileToQLRequest request) {

        log.info("收到快速转译请求");

        String script = transpileService.quickTranspile(request.getFlow());

        return ResponseEntity.ok(ApiResponse.success(script));
    }

    /**
     * 转译为格式化脚本（带注释）
     *
     * POST /api/v1/transpile/formatted
     *
     * @param request 转译请求
     * @return 格式化的脚本字符串
     */
    @PostMapping("/formatted")
    public ResponseEntity<ApiResponse<String>> transpileFormatted(
            @Valid @RequestBody TranspileToQLRequest request) {

        log.info("收到格式化转译请求");

        String script = transpileService.transpileWithFormat(request.getFlow());

        return ResponseEntity.ok(ApiResponse.success(script));
    }

    /**
     * 转译为紧凑脚本
     *
     * POST /api/v1/transpile/compact
     *
     * @param request 转译请求
     * @return 紧凑格式的脚本字符串
     */
    @PostMapping("/compact")
    public ResponseEntity<ApiResponse<String>> transpileCompact(
            @Valid @RequestBody TranspileToQLRequest request) {

        log.info("收到紧凑转译请求");

        String script = transpileService.transpileCompact(request.getFlow());

        return ResponseEntity.ok(ApiResponse.success(script));
    }
}

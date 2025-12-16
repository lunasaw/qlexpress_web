package com.ql.qlexpress.web.controller;

import com.ql.qlexpress.web.model.dto.ApiResponse;
import com.ql.qlexpress.web.model.dto.ParseToVisualRequest;
import com.ql.qlexpress.web.model.dto.ParseToVisualResponse;
import com.ql.qlexpress.web.model.visual.VisualFlowSchema;
import com.ql.qlexpress.web.service.ParseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 解析控制器
 *
 * 提供QL脚本到可视化流程的解析能力
 *
 * @author qlexpress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/parse")
@RequiredArgsConstructor
@Validated
public class ParseController {

    private final ParseService parseService;

    /**
     * 将QL脚本解析为可视化流程
     *
     * POST /api/v1/parse/to-visual
     *
     * @param request 解析请求
     * @return 解析结果
     */
    @PostMapping("/to-visual")
    public ResponseEntity<ApiResponse<ParseToVisualResponse>> parseToVisual(
            @Valid @RequestBody ParseToVisualRequest request) {
        log.info("接收到QL脚本解析请求");

        ParseToVisualResponse result = parseService.parseToVisual(request);

        if (result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.ok(ApiResponse.<ParseToVisualResponse>builder()
                    .success(false)
                    .code("PARSE_ERROR")
                    .message(result.getErrorMessage())
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 快速解析QL脚本
     *
     * POST /api/v1/parse/quick
     *
     * @param script QL脚本内容
     * @return 可视化流程定义
     */
    @PostMapping("/quick")
    public ResponseEntity<ApiResponse<VisualFlowSchema>> quickParse(@RequestBody String script) {
        log.info("接收到快速解析请求");

        try {
            VisualFlowSchema flow = parseService.quickParse(script);
            return ResponseEntity.ok(ApiResponse.success(flow));
        } catch (Exception e) {
            log.error("快速解析失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.<VisualFlowSchema>builder()
                    .success(false)
                    .code("PARSE_ERROR")
                    .message(e.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }
}

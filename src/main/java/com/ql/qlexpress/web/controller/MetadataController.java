package com.ql.qlexpress.web.controller;

import com.ql.qlexpress.web.model.dto.*;
import com.ql.qlexpress.web.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据控制器
 *
 * @author qlexpress
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    /**
     * 获取所有元数据
     */
    @GetMapping
    public ApiResponse<MetadataResponse> getAllMetadata() {
        log.debug("获取所有元数据");
        MetadataResponse metadata = metadataService.getAllMetadata();
        return ApiResponse.success(metadata);
    }

    /**
     * 获取操作符列表
     *
     * @param category 分类过滤
     */
    @GetMapping("/operators")
    public ApiResponse<List<OperatorMetadata>> getOperators(
            @RequestParam(required = false) String category) {
        log.debug("获取操作符列表, category={}", category);
        List<OperatorMetadata> operators = metadataService.getOperators(category);
        return ApiResponse.success(operators);
    }

    /**
     * 获取函数列表
     *
     * @param category 分类过滤
     * @param search   搜索关键字
     */
    @GetMapping("/functions")
    public ApiResponse<List<FunctionMetadata>> getFunctions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        log.debug("获取函数列表, category={}, search={}", category, search);
        List<FunctionMetadata> functions = metadataService.getFunctions(category, search);
        return ApiResponse.success(functions);
    }

    /**
     * 获取节点模板列表
     *
     * @param category 分类过滤
     */
    @GetMapping("/node-templates")
    public ApiResponse<List<NodeTemplate>> getNodeTemplates(
            @RequestParam(required = false) String category) {
        log.debug("获取节点模板列表, category={}", category);
        List<NodeTemplate> templates = metadataService.getNodeTemplates(category);
        return ApiResponse.success(templates);
    }
}

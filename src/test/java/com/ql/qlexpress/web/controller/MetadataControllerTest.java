package com.ql.qlexpress.web.controller;

import com.ql.qlexpress.web.service.MetadataService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MetadataController单元测试
 *
 * @author qlexpress
 */
@WebMvcTest(MetadataController.class)
class MetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataService metadataService;

    @Test
    @DisplayName("GET /api/v1/metadata - 获取所有元数据")
    void getAllMetadata_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("GET /api/v1/metadata/operators - 获取操作符列表")
    void getOperators_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata/operators")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/metadata/operators?category=ARITHMETIC - 按分类获取操作符")
    void getOperators_withCategory_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata/operators")
                        .param("category", "ARITHMETIC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/metadata/functions - 获取函数列表")
    void getFunctions_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata/functions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/metadata/functions?category=数学函数&search=max - 带过滤条件获取函数")
    void getFunctions_withFilters_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata/functions")
                        .param("category", "数学函数")
                        .param("search", "max")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/metadata/node-templates - 获取节点模板列表")
    void getNodeTemplates_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata/node-templates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/metadata/node-templates?category=CONTROL_FLOW - 按分类获取节点模板")
    void getNodeTemplates_withCategory_shouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/metadata/node-templates")
                        .param("category", "CONTROL_FLOW")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

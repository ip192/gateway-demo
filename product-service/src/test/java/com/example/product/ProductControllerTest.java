package com.example.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testAdd_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/product/add")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("产品添加成功"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testQuery_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/product/query")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("产品查询成功"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testAdd_WithMultipleRequests_ShouldReturnConsistentResponse() throws Exception {
        // 测试多次请求的一致性
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/product/add")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("产品添加成功"))
                    .andExpect(jsonPath("$.status").value("success"));
        }
    }

    @Test
    public void testQuery_WithMultipleRequests_ShouldReturnConsistentResponse() throws Exception {
        // 测试多次请求的一致性
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/product/query")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("产品查询成功"))
                    .andExpect(jsonPath("$.status").value("success"));
        }
    }

    @Test
    public void testInfo_WithValidId_ShouldReturnSuccessResponse() throws Exception {
        String requestBody = "{\"id\":\"product-456\"}";
        
        mockMvc.perform(post("/product/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("product info for id: product-456"))
                .andExpect(jsonPath("$.status").value("success"));
    }
}
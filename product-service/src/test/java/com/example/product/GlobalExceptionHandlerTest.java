package com.example.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ProductController.class, GlobalExceptionHandler.class})
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testNotFoundEndpoint_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/product/nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("请求的资源不存在"))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testInvalidHttpMethod_ShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/product/add")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请求方法不支持")))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    public void testInvalidHttpMethod_Query_ShouldReturnMethodNotAllowed() throws Exception {
        mockMvc.perform(get("/product/query")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("请求方法不支持")))
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
package com.example.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLogin_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testLogout_ShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(post("/user/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("登出成功"))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    public void testLogin_WithMultipleRequests_ShouldReturnConsistentResponse() throws Exception {
        // 测试多次请求的一致性
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/user/login")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("登录成功"))
                    .andExpect(jsonPath("$.status").value("success"));
        }
    }

    @Test
    public void testLogout_WithMultipleRequests_ShouldReturnConsistentResponse() throws Exception {
        // 测试多次请求的一致性
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/user/logout")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("登出成功"))
                    .andExpect(jsonPath("$.status").value("success"));
        }
    }

    @Test
    public void testInfo_WithValidId_ShouldReturnSuccessResponse() throws Exception {
        String requestBody = "{\"id\":\"user-123\"}";
        
        mockMvc.perform(post("/user/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("user info for id: user-123"))
                .andExpect(jsonPath("$.status").value("success"));
    }
}
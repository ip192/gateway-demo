package com.example.gateway;

import com.example.gateway.exception.GatewayExceptionHandler;
import com.example.gateway.model.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

public class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler exceptionHandler = new GatewayExceptionHandler();

    @Test
    public void testHandleBadRequest_ShouldReturnBadRequest() {
        IllegalArgumentException exception = new IllegalArgumentException("参数错误");

        ResponseEntity<ApiResponse> response = exceptionHandler.handleBadRequest(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("请求参数错误: 参数错误", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleGenericException_ShouldReturnInternalServerError() {
        RuntimeException exception = new RuntimeException("未知错误");

        ResponseEntity<ApiResponse> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("网关服务错误: 未知错误", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }
}
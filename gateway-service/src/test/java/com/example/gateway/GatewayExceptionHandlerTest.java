package com.example.gateway;

import com.example.gateway.exception.GatewayExceptionHandler;
import com.example.gateway.model.ApiResponse;
import com.example.gateway.service.ServiceDiscoveryException;
import com.example.gateway.service.ProxyException;
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

    // 动态路由异常处理测试

    @Test
    public void testHandleServiceDiscoveryException_ShouldReturnServiceUnavailable() {
        ServiceDiscoveryException exception = new ServiceDiscoveryException("user-service", "服务实例不可用");

        ResponseEntity<ApiResponse> response = exceptionHandler.handleServiceDiscoveryException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("服务发现失败: 服务发现失败 - 服务: user-service, 原因: 服务实例不可用", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleServiceDiscoveryException_WithSimpleMessage_ShouldReturnServiceUnavailable() {
        ServiceDiscoveryException exception = new ServiceDiscoveryException("无法连接到Eureka服务器");

        ResponseEntity<ApiResponse> response = exceptionHandler.handleServiceDiscoveryException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("服务发现失败: 无法连接到Eureka服务器", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleProxyException_WithBadGatewayStatus_ShouldReturnBadGateway() {
        ProxyException exception = ProxyException.forwardingFailed(
            "http://localhost:8081/user/info", 
            new RuntimeException("连接被拒绝")
        );

        ResponseEntity<ApiResponse> response = exceptionHandler.handleProxyException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("请求转发失败: 请求转发失败 - 目标URL: http://localhost:8081/user/info", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleProxyException_WithServiceUnavailableStatus_ShouldReturnServiceUnavailable() {
        ProxyException exception = ProxyException.connectionFailed(
            "http://localhost:8082/product/list", 
            new RuntimeException("服务不可达")
        );

        ResponseEntity<ApiResponse> response = exceptionHandler.handleProxyException(exception);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("请求转发失败: 连接目标服务失败 - 目标URL: http://localhost:8082/product/list", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleProxyException_WithTimeoutStatus_ShouldReturnGatewayTimeout() {
        ProxyException exception = ProxyException.timeout("http://localhost:8081/user/slow-endpoint");

        ResponseEntity<ApiResponse> response = exceptionHandler.handleProxyException(exception);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("请求转发失败: 请求转发超时 - 目标URL: http://localhost:8081/user/slow-endpoint", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleProxyException_WithDefaultStatus_ShouldReturnInternalServerError() {
        ProxyException exception = new ProxyException("未知的转发错误", 999);

        ResponseEntity<ApiResponse> response = exceptionHandler.handleProxyException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("请求转发失败: 未知的转发错误", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testHandleProxyException_WithSimpleMessage_ShouldReturnInternalServerError() {
        ProxyException exception = new ProxyException("请求处理失败");

        ResponseEntity<ApiResponse> response = exceptionHandler.handleProxyException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("请求转发失败: 请求处理失败", response.getBody().getMessage());
        assertEquals("error", response.getBody().getStatus());
    }

    @Test
    public void testProxyExceptionStatusCodeMapping_ShouldMapCorrectly() {
        // 测试502状态码映射
        ProxyException badGatewayException = new ProxyException("Bad Gateway", 502);
        ResponseEntity<ApiResponse> badGatewayResponse = exceptionHandler.handleProxyException(badGatewayException);
        assertEquals(HttpStatus.BAD_GATEWAY, badGatewayResponse.getStatusCode());

        // 测试503状态码映射
        ProxyException serviceUnavailableException = new ProxyException("Service Unavailable", 503);
        ResponseEntity<ApiResponse> serviceUnavailableResponse = exceptionHandler.handleProxyException(serviceUnavailableException);
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, serviceUnavailableResponse.getStatusCode());

        // 测试504状态码映射
        ProxyException gatewayTimeoutException = new ProxyException("Gateway Timeout", 504);
        ResponseEntity<ApiResponse> gatewayTimeoutResponse = exceptionHandler.handleProxyException(gatewayTimeoutException);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, gatewayTimeoutResponse.getStatusCode());

        // 测试其他状态码映射到500
        ProxyException unknownException = new ProxyException("Unknown Error", 418);
        ResponseEntity<ApiResponse> unknownResponse = exceptionHandler.handleProxyException(unknownException);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, unknownResponse.getStatusCode());
    }
}
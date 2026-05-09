package com.example.gateway.exception;

import com.example.gateway.model.ApiResponse;
import com.example.gateway.service.ServiceDiscoveryException;
import com.example.gateway.service.ProxyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GatewayExceptionHandler {

    @ExceptionHandler(ServiceDiscoveryException.class)
    public ResponseEntity<ApiResponse> handleServiceDiscoveryException(ServiceDiscoveryException ex) {
        ApiResponse response = new ApiResponse("服务发现失败: " + ex.getMessage(), "error");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ProxyException.class)
    public ResponseEntity<ApiResponse> handleProxyException(ProxyException ex) {
        ApiResponse response = new ApiResponse("请求转发失败: " + ex.getMessage(), "error");
        HttpStatus status = mapProxyExceptionToHttpStatus(ex.getStatusCode());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(IllegalArgumentException ex) {
        ApiResponse response = new ApiResponse("请求参数错误: " + ex.getMessage(), "error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex) {
        ApiResponse response = new ApiResponse("网关服务错误: " + ex.getMessage(), "error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 将ProxyException的状态码映射到HttpStatus
     */
    private HttpStatus mapProxyExceptionToHttpStatus(int statusCode) {
        switch (statusCode) {
            case 502:
                return HttpStatus.BAD_GATEWAY;
            case 503:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case 504:
                return HttpStatus.GATEWAY_TIMEOUT;
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}
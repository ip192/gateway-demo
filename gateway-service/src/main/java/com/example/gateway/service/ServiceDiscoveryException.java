package com.example.gateway.service;

/**
 * 服务发现异常类
 * 当服务发现过程中出现问题时抛出此异常
 */
public class ServiceDiscoveryException extends RuntimeException {
    
    public ServiceDiscoveryException(String message) {
        super(message);
    }
    
    public ServiceDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ServiceDiscoveryException(String serviceName, String reason) {
        super(String.format("服务发现失败 - 服务: %s, 原因: %s", serviceName, reason));
    }
}
package com.example.gateway.service;

/**
 * 请求转发异常类
 * 当HTTP请求转发过程中出现问题时抛出此异常
 */
public class ProxyException extends RuntimeException {
    
    private final int statusCode;
    private final String targetUrl;
    
    public ProxyException(String message) {
        super(message);
        this.statusCode = 500;
        this.targetUrl = null;
    }
    
    public ProxyException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
        this.targetUrl = null;
    }
    
    public ProxyException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.targetUrl = null;
    }
    
    public ProxyException(String message, int statusCode, String targetUrl) {
        super(message);
        this.statusCode = statusCode;
        this.targetUrl = targetUrl;
    }
    
    public ProxyException(String message, int statusCode, String targetUrl, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.targetUrl = targetUrl;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getTargetUrl() {
        return targetUrl;
    }
    
    /**
     * 创建请求转发失败异常
     */
    public static ProxyException forwardingFailed(String targetUrl, Throwable cause) {
        return new ProxyException(
            String.format("请求转发失败 - 目标URL: %s", targetUrl), 
            502, 
            targetUrl, 
            cause
        );
    }
    
    /**
     * 创建超时异常
     */
    public static ProxyException timeout(String targetUrl) {
        return new ProxyException(
            String.format("请求转发超时 - 目标URL: %s", targetUrl), 
            504, 
            targetUrl
        );
    }
    
    /**
     * 创建连接失败异常
     */
    public static ProxyException connectionFailed(String targetUrl, Throwable cause) {
        return new ProxyException(
            String.format("连接目标服务失败 - 目标URL: %s", targetUrl), 
            503, 
            targetUrl, 
            cause
        );
    }
}
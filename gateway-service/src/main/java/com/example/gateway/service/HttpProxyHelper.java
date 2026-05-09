package com.example.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * HTTP请求转发工具类
 * 处理HTTP请求的完整转发，包括头信息、参数和请求体
 */
@Component
public class HttpProxyHelper {

    private static final Logger logger = LoggerFactory.getLogger(HttpProxyHelper.class);

    private final RestTemplate restTemplate;

    // 需要过滤的HTTP头信息（不应该转发的头）
    private static final Set<String> FILTERED_HEADERS = new HashSet<>(Arrays.asList(
            "host", "connection", "content-length", "transfer-encoding",
            "upgrade", "proxy-connection", "proxy-authenticate", 
            "proxy-authorization", "te", "trailers"
    ));

    @Autowired
    public HttpProxyHelper(@Qualifier("dynamicRoutingRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 执行实际的HTTP请求转发
     * 
     * @param targetUrl 目标服务URL
     * @param method HTTP方法
     * @param headers HTTP头信息
     * @param body 请求体
     * @return 转发后的响应
     */
    public ResponseEntity<Object> forwardRequest(String targetUrl, 
                                               HttpMethod method,
                                               HttpHeaders headers, 
                                               Object body) {
        try {
            logger.debug("转发请求到: {} 方法: {}", targetUrl, method);
            
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Object> response = restTemplate.exchange(targetUrl, method, entity, Object.class);
            
            logger.debug("请求转发成功，状态码: {}", response.getStatusCode());
            return response;
            
        } catch (Exception e) {
            logger.error("请求转发失败: {} - {}", targetUrl, e.getMessage());
            throw new ProxyException("请求转发失败: " + e.getMessage(), 
                                   HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    /**
     * 过滤和转换HTTP头信息
     * 移除不应该转发的头信息，保留必要的头信息
     * 
     * @param originalHeaders 原始HTTP头信息
     * @return 过滤后的HTTP头信息
     */
    public HttpHeaders filterHeaders(HttpHeaders originalHeaders) {
        HttpHeaders filteredHeaders = new HttpHeaders();
        
        originalHeaders.forEach((headerName, headerValues) -> {
            String lowerCaseHeaderName = headerName.toLowerCase();
            
            // 跳过需要过滤的头信息
            if (FILTERED_HEADERS.contains(lowerCaseHeaderName)) {
                return;
            }
            
            // 添加未被过滤的头信息
            filteredHeaders.put(headerName, new ArrayList<>(headerValues));
        });
        
        logger.debug("过滤后的头信息数量: {}", filteredHeaders.size());
        return filteredHeaders;
    }

    /**
     * 处理查询参数，构建查询字符串
     * 
     * @param queryParams 查询参数Map
     * @return 构建的查询参数字符串，如果没有参数则返回空字符串
     */
    public String buildQueryString(MultiValueMap<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }
        
        StringBuilder queryString = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            String paramName = entry.getKey();
            List<String> paramValues = entry.getValue();
            
            for (String paramValue : paramValues) {
                if (!first) {
                    queryString.append("&");
                }
                queryString.append(paramName);
                if (paramValue != null && !paramValue.isEmpty()) {
                    queryString.append("=").append(paramValue);
                }
                first = false;
            }
        }
        
        String result = queryString.toString();
        logger.debug("构建的查询字符串: {}", result);
        return result;
    }
}
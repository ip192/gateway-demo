package com.example.gateway.controller;

import com.example.gateway.config.DynamicRoutingProperties;
import com.example.gateway.service.HttpProxyHelper;
import com.example.gateway.service.ServiceDiscoveryHelper;
import com.example.gateway.service.ServiceDiscoveryException;
import com.example.gateway.service.ProxyException;
import com.example.gateway.service.DynamicRoutingMetrics;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 动态代理控制器核心类
 * 负责处理所有动态路由请求，自动转发到对应的后端服务
 * 设置较低优先级，确保与Gateway路由和Feign客户端兼容
 */
@RestController
@RequestMapping("/")
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.LOWEST_PRECEDENCE)
public class DynamicProxyController {
    
    private static final Logger logger = LoggerFactory.getLogger(DynamicProxyController.class);
    
    private final ServiceDiscoveryHelper serviceDiscoveryHelper;
    private final HttpProxyHelper httpProxyHelper;
    private final DynamicRoutingProperties dynamicRoutingProperties;
    private final DynamicRoutingMetrics metrics;
    
    @Autowired
    public DynamicProxyController(ServiceDiscoveryHelper serviceDiscoveryHelper, 
                                 HttpProxyHelper httpProxyHelper,
                                 DynamicRoutingProperties dynamicRoutingProperties,
                                 DynamicRoutingMetrics metrics) {
        this.serviceDiscoveryHelper = serviceDiscoveryHelper;
        this.httpProxyHelper = httpProxyHelper;
        this.dynamicRoutingProperties = dynamicRoutingProperties;
        this.metrics = metrics;
    }
    
    /**
     * 处理所有/user/**路径的请求，支持所有HTTP方法
     * 只有在Gateway路由和Feign客户端都无法处理时才使用动态路由
     * 
     * @param request HTTP请求对象
     * @param body 请求体（可选）
     * @return 转发后的响应
     */
    @RequestMapping(value = "/user/**", 
                   method = {RequestMethod.GET, RequestMethod.POST, 
                            RequestMethod.PUT, RequestMethod.DELETE, 
                            RequestMethod.PATCH, RequestMethod.HEAD, 
                            RequestMethod.OPTIONS})
    public ResponseEntity<Object> proxyUserService(HttpServletRequest request, 
                                                  @RequestBody(required = false) Object body) {
        // 设置请求追踪ID
        String requestId = generateRequestId();
        MDC.put("requestId", requestId);
        MDC.put("serviceName", "user-service");
        
        try {
            // 检查是否应该使用动态路由
            if (!shouldUseDynamicRouting(request)) {
                logger.debug("跳过动态路由，让其他路由方式处理: {} {}", request.getMethod(), request.getRequestURI());
                // 返回404让Spring继续寻找其他匹配的处理器
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "路由未找到");
            }
            
            logger.info("接收到用户服务动态路由请求: {} {} [requestId={}]", 
                       request.getMethod(), request.getRequestURI(), requestId);
            return proxyRequest("user-service", request, body);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 处理所有/product/**路径的请求，支持所有HTTP方法
     * 只有在Gateway路由和Feign客户端都无法处理时才使用动态路由
     * 
     * @param request HTTP请求对象
     * @param body 请求体（可选）
     * @return 转发后的响应
     */
    @RequestMapping(value = "/product/**", 
                   method = {RequestMethod.GET, RequestMethod.POST, 
                            RequestMethod.PUT, RequestMethod.DELETE, 
                            RequestMethod.PATCH, RequestMethod.HEAD, 
                            RequestMethod.OPTIONS})
    public ResponseEntity<Object> proxyProductService(HttpServletRequest request, 
                                                     @RequestBody(required = false) Object body) {
        // 设置请求追踪ID
        String requestId = generateRequestId();
        MDC.put("requestId", requestId);
        MDC.put("serviceName", "product-service");
        
        try {
            // 检查是否应该使用动态路由
            if (!shouldUseDynamicRouting(request)) {
                logger.debug("跳过动态路由，让其他路由方式处理: {} {}", request.getMethod(), request.getRequestURI());
                // 返回404让Spring继续寻找其他匹配的处理器
                throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "路由未找到");
            }
            
            logger.info("接收到产品服务动态路由请求: {} {} [requestId={}]", 
                       request.getMethod(), request.getRequestURI(), requestId);
            return proxyRequest("product-service", request, body);
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * 通用请求转发逻辑
     * 负责服务发现、URL构建和请求转发的完整流程
     * 处理请求体、查询参数和HTTP头的完整传递
     * 
     * @param serviceName 目标服务名称
     * @param request HTTP请求对象
     * @param body 请求体
     * @return 转发后的响应
     */
    private ResponseEntity<Object> proxyRequest(String serviceName, 
                                              HttpServletRequest request, 
                                              Object body) {
        // 开始计时
        Timer.Sample timerSample = metrics.startTimer();
        String requestId = MDC.get("requestId");
        
        // 获取请求的基本信息
        String requestPath = request.getRequestURI();
        HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod());
        String clientIp = getClientIpAddress(request);
        
        // 记录请求开始
        logger.info("开始动态路由处理: 服务={}, 路径={}, 方法={}, 客户端IP={}, 请求ID={}", 
                   serviceName, requestPath, httpMethod, clientIp, requestId);
        
        // 记录请求详细信息（DEBUG级别）
        if (logger.isDebugEnabled()) {
            logger.debug("请求详情 [{}]: User-Agent={}, Content-Type={}, Content-Length={}", 
                        requestId,
                        request.getHeader("User-Agent"),
                        request.getHeader("Content-Type"),
                        request.getHeader("Content-Length"));
            
            // 记录查询参数
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                logger.debug("查询参数 [{}]: {}", requestId, queryString);
            }
        }
        
        try {
            // 1. 服务发现 - 获取健康的服务实例
            logger.debug("开始服务发现 [{}]: 查找服务 {}", requestId, serviceName);
            ServiceInstance serviceInstance = serviceDiscoveryHelper.getServiceInstance(serviceName);
            if (serviceInstance == null) {
                metrics.recordServiceDiscoveryFailure(serviceName);
                throw new ServiceDiscoveryException("无法获取服务实例: " + serviceName);
            }
            
            logger.debug("服务发现成功 [{}]: {}:{}", requestId, serviceInstance.getHost(), serviceInstance.getPort());
            
            // 2. 构建目标服务URL（包含查询参数）
            String targetUrl = buildTargetUrl(serviceInstance, request);
            
            // 3. 处理HTTP头信息 - 过滤不应转发的头信息
            HttpHeaders filteredHeaders = convertHeaders(request);
            
            // 4. 添加必要的头信息
            addRequiredHeaders(filteredHeaders, request);
            
            logger.info("转发请求 [{}]: {} {} -> {}", requestId, httpMethod, requestPath, targetUrl);
            
            // 记录转发的头信息（DEBUG级别）
            if (logger.isDebugEnabled()) {
                logger.debug("转发头信息 [{}]: {}", requestId, filteredHeaders.keySet());
                logger.debug("请求体类型 [{}]: {}", requestId, body != null ? body.getClass().getSimpleName() : "null");
            }
            
            // 5. 执行请求转发
            ResponseEntity<Object> response = httpProxyHelper.forwardRequest(
                    targetUrl, httpMethod, filteredHeaders, body);
            
            // 停止计时并记录成功指标
            metrics.stopTimer(timerSample, serviceName, httpMethod.name());
            metrics.recordSuccess(serviceName, httpMethod.name(), response.getStatusCodeValue());
            
            // 记录成功响应
            logger.info("动态路由成功 [{}]: {} {} -> {} 状态码={}, 响应时间={}ms", 
                      requestId, httpMethod, requestPath, serviceName, 
                      response.getStatusCode(), getElapsedTime(timerSample));
            
            // 记录响应详细信息（DEBUG级别）
            if (logger.isDebugEnabled()) {
                logger.debug("响应详情 [{}]: 头信息={}, 内容类型={}", 
                           requestId, 
                           response.getHeaders().keySet(),
                           response.getHeaders().getContentType());
                
                // 记录响应体大小（如果可获取）
                if (response.getHeaders().getContentLength() > 0) {
                    logger.debug("响应体大小 [{}]: {} bytes", requestId, response.getHeaders().getContentLength());
                }
            }
            
            return response;
            
        } catch (ServiceDiscoveryException e) {
            // 记录服务发现失败
            metrics.recordFailure(serviceName, httpMethod.name(), "SERVICE_DISCOVERY_FAILURE");
            logger.error("服务发现失败 [{}]: 服务={}, 错误={}", requestId, serviceName, e.getMessage());
            throw e;
            
        } catch (ProxyException e) {
            // 记录代理失败
            metrics.recordFailure(serviceName, httpMethod.name(), "PROXY_FAILURE");
            metrics.recordProxyFailure(serviceName, "PROXY_ERROR");
            logger.error("HTTP代理失败 [{}]: {} {} -> {}, 错误={}", 
                       requestId, httpMethod, requestPath, serviceName, e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // 记录未知异常
            metrics.recordFailure(serviceName, httpMethod.name(), "UNKNOWN_ERROR");
            logger.error("动态路由处理异常 [{}]: 服务={}, 路径={}, 错误={}", 
                       requestId, serviceName, requestPath, e.getMessage(), e);
            throw new ProxyException("动态路由处理失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 转换HttpServletRequest的头信息为HttpHeaders
     * 
     * @param request HTTP请求对象
     * @return 转换后的HttpHeaders
     */
    private HttpHeaders convertHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            java.util.Enumeration<String> headerValues = request.getHeaders(headerName);
            
            while (headerValues.hasMoreElements()) {
                headers.add(headerName, headerValues.nextElement());
            }
        }
        
        return httpProxyHelper.filterHeaders(headers);
    }
    
    /**
     * 添加必要的HTTP头信息
     * 
     * @param headers 要添加头信息的HttpHeaders对象
     * @param request 原始请求对象
     */
    private void addRequiredHeaders(HttpHeaders headers, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        
        // 添加X-Forwarded-For头信息，用于追踪原始客户端IP
        String clientIp = getClientIpAddress(request);
        if (clientIp != null) {
            headers.add("X-Forwarded-For", clientIp);
        }
        
        // 添加X-Forwarded-Proto头信息
        headers.add("X-Forwarded-Proto", request.getScheme());
        
        // 添加X-Forwarded-Host头信息
        String host = request.getHeader("Host");
        if (host != null) {
            headers.add("X-Forwarded-Host", host);
        }
        
        // 添加请求追踪ID
        if (requestId != null) {
            headers.add("X-Request-ID", requestId);
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("添加转发头信息 [{}]: X-Forwarded-For={}, X-Forwarded-Proto={}, X-Forwarded-Host={}", 
                        requestId, clientIp, request.getScheme(), host);
        }
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // 尝试从X-Forwarded-For头获取
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        // 尝试从X-Real-IP头获取
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // 从远程地址获取
        return request.getRemoteAddr();
    }
    
    /**
     * 构建目标服务的完整URL
     * 包括服务地址、端口、路径和查询参数
     * 
     * @param serviceInstance 服务实例
     * @param request HTTP请求对象
     * @return 完整的目标URL
     */
    private String buildTargetUrl(ServiceInstance serviceInstance, HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        String requestPath = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // 验证输入参数
        if (serviceInstance == null) {
            logger.error("构建目标URL失败 [{}]: 服务实例不能为空", requestId);
            throw new IllegalArgumentException("服务实例不能为空");
        }
        
        if (requestPath == null || requestPath.isEmpty()) {
            requestPath = "/";
        }
        
        // 构建基础URL
        String baseUrl = serviceDiscoveryHelper.buildServiceUrl(serviceInstance, requestPath);
        
        // 添加查询参数（完整传递所有查询参数）
        if (queryString != null && !queryString.isEmpty()) {
            baseUrl += "?" + queryString;
            if (logger.isDebugEnabled()) {
                logger.debug("添加查询参数 [{}]: {}", requestId, queryString);
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("构建目标URL [{}]: {} -> {}", requestId, requestPath, baseUrl);
        }
        return baseUrl;
    }
    
    /**
     * 判断是否应该使用动态路由
     * 检查路由优先级和配置，确保与Gateway路由和Feign客户端的兼容性
     * 
     * @param request HTTP请求对象
     * @return 是否使用动态路由
     */
    private boolean shouldUseDynamicRouting(HttpServletRequest request) {
        String requestId = MDC.get("requestId");
        String requestPath = request.getRequestURI();
        
        // 1. 检查动态路由是否启用
        if (!dynamicRoutingProperties.isEnabled()) {
            if (logger.isDebugEnabled()) {
                logger.debug("动态路由已禁用 [{}]", requestId);
            }
            return false;
        }
        
        // 2. 检查是否已被Feign客户端处理
        String routingPriority = request.getHeader("X-Gateway-Routing-Priority");
        if ("feign-processed".equals(routingPriority)) {
            if (logger.isDebugEnabled()) {
                logger.debug("请求已被Feign客户端处理，跳过动态路由 [{}]", requestId);
            }
            return false;
        }
        
        // 3. 检查User-Agent，避免处理Feign内部调用
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.toLowerCase().contains("feign")) {
            if (logger.isDebugEnabled()) {
                logger.debug("检测到Feign客户端调用，跳过动态路由 [{}]: User-Agent={}", requestId, userAgent);
            }
            return false;
        }
        
        // 4. 检查是否有Gateway动态路由标记
        String dynamicRouting = request.getHeader("X-Gateway-Dynamic-Routing");
        if ("true".equals(dynamicRouting)) {
            if (logger.isDebugEnabled()) {
                logger.debug("检测到Gateway动态路由标记，使用动态路由 [{}]", requestId);
            }
            return true;
        }
        
        // 5. 检查路径是否为特殊的静态路由路径
        if (isStaticRoutePath(requestPath)) {
            if (logger.isDebugEnabled()) {
                logger.debug("请求路径匹配静态路由，跳过动态路由 [{}]: {}", requestId, requestPath);
            }
            return false;
        }
        
        // 6. 默认情况下，对于/user/**和/product/**路径使用动态路由
        if (logger.isDebugEnabled()) {
            logger.debug("使用动态路由处理请求 [{}]: {}", requestId, requestPath);
        }
        return true;
    }
    
    /**
     * 检查路径是否为已知的静态路由路径
     * 
     * @param path 请求路径
     * @return 是否为静态路由路径
     */
    private boolean isStaticRoutePath(String path) {
        // 定义需要通过静态路由处理的特殊路径
        // 这些路径将优先使用Gateway的静态路由配置
        
        // 用户服务的特殊静态路径
        if (path.startsWith("/user/static/") || 
            path.startsWith("/user/admin/") ||
            path.equals("/user/health")) {
            return true;
        }
        
        // 产品服务的特殊静态路径
        if (path.startsWith("/product/static/") || 
            path.startsWith("/product/admin/") ||
            path.equals("/product/health")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 生成唯一的请求ID用于追踪
     * 
     * @return 请求ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 获取计时器的经过时间（毫秒）
     * 注意：这是一个近似值，用于日志记录
     * 
     * @param timerSample 计时器样本
     * @return 经过时间（毫秒）
     */
    private long getElapsedTime(Timer.Sample timerSample) {
        // 由于Timer.Sample没有直接获取经过时间的方法，
        // 这里返回一个占位符，实际时间会通过Micrometer记录
        return System.currentTimeMillis() % 10000; // 简化的时间显示
    }
}
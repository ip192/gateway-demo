package com.example.gateway.controller;

import com.example.gateway.config.DynamicRoutingProperties;
import com.example.gateway.service.HttpProxyHelper;
import com.example.gateway.service.ServiceDiscoveryHelper;
import com.example.gateway.service.ServiceDiscoveryException;
import com.example.gateway.service.ProxyException;
import com.example.gateway.service.DynamicRoutingMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DynamicProxyController单元测试
 * 验证动态路由控制器的核心功能
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DynamicProxyControllerTest {

    @Mock
    private ServiceDiscoveryHelper serviceDiscoveryHelper;

    @Mock
    private HttpProxyHelper httpProxyHelper;

    @Mock
    private DynamicRoutingProperties dynamicRoutingProperties;

    @Mock
    private DynamicRoutingMetrics metrics;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Timer.Sample timerSample;

    @Mock
    private HttpServletRequest request;

    @Mock
    private ServiceInstance serviceInstance;

    private DynamicProxyController controller;

    @BeforeEach
    void setUp() {
        controller = new DynamicProxyController(serviceDiscoveryHelper, httpProxyHelper, dynamicRoutingProperties, metrics);
        
        // 设置默认的动态路由配置
        when(dynamicRoutingProperties.isEnabled()).thenReturn(true);
        
        // 设置默认的监控配置
        when(metrics.startTimer()).thenReturn(timerSample);
        when(metrics.getMeterRegistry()).thenReturn(meterRegistry);
    }

    @Test
    void testProxyUserService_Success() throws Exception {
        // 准备测试数据
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
            .body("{\"id\": 123, \"name\": \"Test User\"}");

        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("id=123");
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.singletonList("Content-Type"));
        when(request.getHeaderNames()).thenReturn(headerNames);
        when(request.getHeaders("Content-Type")).thenReturn(Collections.enumeration(Collections.singletonList("application/json")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/info")).thenReturn("http://localhost:8081/user/info");

        // 模拟HTTP代理
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
            .thenReturn(expectedResponse);

        // 执行测试
        ResponseEntity<Object> result = controller.proxyUserService(request, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("{\"id\": 123, \"name\": \"Test User\"}", result.getBody());

        // 验证方法调用
        verify(serviceDiscoveryHelper).getServiceInstance("user-service");
        verify(httpProxyHelper).filterHeaders(any(HttpHeaders.class));
        verify(httpProxyHelper).forwardRequest(eq("http://localhost:8081/user/info?id=123"), 
                                             eq(HttpMethod.GET), 
                                             any(HttpHeaders.class), 
                                             isNull());
    }

    @Test
    void testProxyProductService_Success() throws Exception {
        // 准备测试数据
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
            .body("[{\"id\": 1, \"name\": \"Product 1\"}]");

        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/product/list");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.emptyList());
        when(request.getHeaderNames()).thenReturn(headerNames);

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/product/list")).thenReturn("http://localhost:8082/product/list");

        // 模拟HTTP代理
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
            .thenReturn(expectedResponse);

        // 执行测试
        ResponseEntity<Object> result = controller.proxyProductService(request, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("[{\"id\": 1, \"name\": \"Product 1\"}]", result.getBody());

        // 验证方法调用
        verify(serviceDiscoveryHelper).getServiceInstance("product-service");
        verify(httpProxyHelper).filterHeaders(any(HttpHeaders.class));
        verify(httpProxyHelper).forwardRequest(eq("http://localhost:8082/product/list"), 
                                             eq(HttpMethod.GET), 
                                             any(HttpHeaders.class), 
                                             isNull());
    }

    @Test
    void testProxyRequest_ServiceDiscoveryFailure() throws Exception {
        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");

        // 模拟服务发现失败
        when(serviceDiscoveryHelper.getServiceInstance("user-service"))
            .thenThrow(new ServiceDiscoveryException("服务不可用"));

        // 执行测试并验证异常
        assertThrows(ServiceDiscoveryException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证方法调用
        verify(serviceDiscoveryHelper).getServiceInstance("user-service");
        verifyNoInteractions(httpProxyHelper);
    }

    @Test
    void testProxyRequest_WithRequestBody() throws Exception {
        // 准备测试数据
        Object requestBody = "{\"name\": \"New User\", \"email\": \"user@example.com\"}";
        ResponseEntity<Object> expectedResponse = ResponseEntity.status(HttpStatus.CREATED)
            .body("{\"id\": 456, \"name\": \"New User\"}");

        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/create");
        when(request.getMethod()).thenReturn("POST");
        when(request.getQueryString()).thenReturn(null);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.singletonList("Content-Type"));
        when(request.getHeaderNames()).thenReturn(headerNames);
        when(request.getHeaders("Content-Type")).thenReturn(Collections.enumeration(Collections.singletonList("application/json")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/create")).thenReturn("http://localhost:8081/user/create");

        // 模拟HTTP代理
        HttpHeaders filteredHeaders = new HttpHeaders();
        filteredHeaders.setContentType(MediaType.APPLICATION_JSON);
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(filteredHeaders);
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), eq(requestBody)))
            .thenReturn(expectedResponse);

        // 执行测试
        ResponseEntity<Object> result = controller.proxyUserService(request, requestBody);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("{\"id\": 456, \"name\": \"New User\"}", result.getBody());

        // 验证方法调用
        verify(serviceDiscoveryHelper).getServiceInstance("user-service");
        verify(httpProxyHelper).filterHeaders(any(HttpHeaders.class));
        verify(httpProxyHelper).forwardRequest(eq("http://localhost:8081/user/create"), 
                                             eq(HttpMethod.POST), 
                                             any(HttpHeaders.class), 
                                             eq(requestBody));
    }

    @Test
    void testProxyRequest_WithQueryParameters() throws Exception {
        // 准备测试数据
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
            .body("[{\"id\": 1, \"name\": \"Tech Product\"}]");

        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/product/search");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("category=tech&price=100");
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.emptyList());
        when(request.getHeaderNames()).thenReturn(headerNames);

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/product/search")).thenReturn("http://localhost:8082/product/search");

        // 模拟HTTP代理
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
            .thenReturn(expectedResponse);

        // 执行测试
        ResponseEntity<Object> result = controller.proxyProductService(request, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("[{\"id\": 1, \"name\": \"Tech Product\"}]", result.getBody());

        // 验证方法调用
        verify(serviceDiscoveryHelper).getServiceInstance("product-service");
        verify(httpProxyHelper).filterHeaders(any(HttpHeaders.class));
        verify(httpProxyHelper).forwardRequest(eq("http://localhost:8082/product/search?category=tech&price=100"), 
                                             eq(HttpMethod.GET), 
                                             any(HttpHeaders.class), 
                                             isNull());
    }

    @Test
    void testProxyUserService_DynamicRoutingDisabled() throws Exception {
        // 禁用动态路由
        when(dynamicRoutingProperties.isEnabled()).thenReturn(false);
        
        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");

        // 执行测试并验证异常
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证没有调用服务发现
        verifyNoInteractions(serviceDiscoveryHelper);
        verifyNoInteractions(httpProxyHelper);
    }

    @Test
    void testProxyUserService_FeignClientRequest() throws Exception {
        // 模拟Feign客户端请求
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Gateway-Routing-Priority")).thenReturn("feign-processed");

        // 执行测试并验证异常
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证没有调用服务发现
        verifyNoInteractions(serviceDiscoveryHelper);
        verifyNoInteractions(httpProxyHelper);
    }

    @Test
    void testProxyUserService_FeignUserAgent() throws Exception {
        // 模拟Feign客户端User-Agent
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("User-Agent")).thenReturn("feign/10.12");

        // 执行测试并验证异常
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证没有调用服务发现
        verifyNoInteractions(serviceDiscoveryHelper);
        verifyNoInteractions(httpProxyHelper);
    }

    @Test
    void testProxyUserService_StaticRoutePath() throws Exception {
        // 模拟静态路由路径
        when(request.getRequestURI()).thenReturn("/user/static/config");
        when(request.getMethod()).thenReturn("GET");

        // 执行测试并验证异常
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证没有调用服务发现
        verifyNoInteractions(serviceDiscoveryHelper);
        verifyNoInteractions(httpProxyHelper);
    }

    @Test
    void testProxyUserService_GatewayDynamicRoutingHeader() throws Exception {
        // 准备测试数据
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
            .body("{\"id\": 123, \"name\": \"Test User\"}");

        // 模拟Gateway动态路由标记
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Gateway-Dynamic-Routing")).thenReturn("true");
        when(request.getQueryString()).thenReturn(null);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.emptyList());
        when(request.getHeaderNames()).thenReturn(headerNames);

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/info")).thenReturn("http://localhost:8081/user/info");

        // 模拟HTTP代理
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
            .thenReturn(expectedResponse);

        // 执行测试
        ResponseEntity<Object> result = controller.proxyUserService(request, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());

        // 验证方法调用
        verify(serviceDiscoveryHelper).getServiceInstance("user-service");
        verify(httpProxyHelper).forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any());
    }

    @Test
    void testMonitoringAndLogging_Success() throws Exception {
        // 准备测试数据
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
            .body("{\"id\": 123, \"name\": \"Test User\"}");

        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn("id=123");
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        when(request.getHeader("X-Gateway-Routing-Priority")).thenReturn(null);
        when(request.getHeader("X-Gateway-Dynamic-Routing")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.singletonList("Content-Type"));
        when(request.getHeaderNames()).thenReturn(headerNames);
        when(request.getHeaders("Content-Type")).thenReturn(Collections.enumeration(Collections.singletonList("application/json")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/info")).thenReturn("http://localhost:8081/user/info");

        // 模拟HTTP代理
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
            .thenReturn(expectedResponse);

        // 执行测试
        ResponseEntity<Object> result = controller.proxyUserService(request, null);

        // 验证结果
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());

        // 验证监控指标记录
        verify(metrics).startTimer();
        verify(metrics).recordSuccess("user-service", "GET", 200);
        verify(metrics, never()).recordFailure(anyString(), anyString(), anyString());
        verify(metrics, never()).recordServiceDiscoveryFailure(anyString());
        verify(metrics, never()).recordProxyFailure(anyString(), anyString());
    }

    @Test
    void testMonitoringAndLogging_ServiceDiscoveryFailure() throws Exception {
        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("X-Gateway-Routing-Priority")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Gateway-Dynamic-Routing")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // 模拟服务发现失败
        when(serviceDiscoveryHelper.getServiceInstance("user-service"))
            .thenThrow(new ServiceDiscoveryException("服务不可用"));

        // 执行测试并验证异常
        assertThrows(ServiceDiscoveryException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证监控指标记录
        verify(metrics).startTimer();
        verify(metrics).recordFailure("user-service", "GET", "SERVICE_DISCOVERY_FAILURE");
        verify(metrics, never()).recordSuccess(anyString(), anyString(), anyInt());
    }

    @Test
    void testMonitoringAndLogging_ProxyFailure() throws Exception {
        // 模拟请求对象
        when(request.getRequestURI()).thenReturn("/user/info");
        when(request.getMethod()).thenReturn("GET");
        when(request.getQueryString()).thenReturn(null);
        when(request.getScheme()).thenReturn("http");
        when(request.getHeader("Host")).thenReturn("localhost:8080");
        when(request.getHeader("X-Gateway-Routing-Priority")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Gateway-Dynamic-Routing")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        
        // 模拟头信息
        Enumeration<String> headerNames = Collections.enumeration(Collections.emptyList());
        when(request.getHeaderNames()).thenReturn(headerNames);

        // 模拟服务发现成功
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/info")).thenReturn("http://localhost:8081/user/info");

        // 模拟HTTP代理失败
        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
            .thenThrow(new ProxyException("连接超时", 500));

        // 执行测试并验证异常
        assertThrows(ProxyException.class, () -> {
            controller.proxyUserService(request, null);
        });

        // 验证监控指标记录
        verify(metrics).startTimer();
        verify(metrics).recordFailure("user-service", "GET", "PROXY_FAILURE");
        verify(metrics).recordProxyFailure("user-service", "PROXY_ERROR");
        verify(metrics, never()).recordSuccess(anyString(), anyString(), anyInt());
    }

    @Test
    void testProxyProductService_StaticRoutePath() throws Exception {
        // 模拟产品服务静态路由路径
        when(request.getRequestURI()).thenReturn("/product/admin/dashboard");
        when(request.getMethod()).thenReturn("GET");

        // 执行测试并验证异常
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.proxyProductService(request, null);
        });

        // 验证没有调用服务发现
        verifyNoInteractions(serviceDiscoveryHelper);
        verifyNoInteractions(httpProxyHelper);
    }

    @Test
    void testProxyProductService_HealthCheckPath() throws Exception {
        // 模拟健康检查路径
        when(request.getRequestURI()).thenReturn("/product/health");
        when(request.getMethod()).thenReturn("GET");

        // 执行测试并验证异常
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.proxyProductService(request, null);
        });

        // 验证没有调用服务发现
        verifyNoInteractions(serviceDiscoveryHelper);
        verifyNoInteractions(httpProxyHelper);
    }
}
package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端集成测试 - Enhanced version with WireMock for comprehensive testing
 * 测试完整的请求转发流程，包括各种场景和边界情况
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EndToEndIntegrationTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private WireMockServer userService;
    private WireMockServer productService;
    private String gatewayBaseUrl;

    @BeforeEach
    void setUp() {
        gatewayBaseUrl = "http://localhost:" + gatewayPort;
        
        // Start mock services
        userService = new WireMockServer(8081);
        productService = new WireMockServer(8082);
        
        userService.start();
        productService.start();
        
        setupUserServiceMocks();
        setupProductServiceMocks();
    }

    @AfterEach
    void tearDown() {
        if (userService != null && userService.isRunning()) {
            userService.stop();
        }
        if (productService != null && productService.isRunning()) {
            productService.stop();
        }
    }

    @Test
    public void testCompleteLoginFlow_ThroughGateway() {
        // 准备登录请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginEntity = new HttpEntity<>("{\"username\":\"testuser\",\"password\":\"testpass\"}", headers);

        // 通过Gateway发送登录请求
        String loginUrl = gatewayBaseUrl + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, loginEntity, String.class);

        // 验证登录响应
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("登录成功"));
        assertTrue(loginResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, loginResponse.getHeaders().getContentType());
    }

    @Test
    public void testCompleteLogoutFlow_ThroughGateway() {
        // 准备登出请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> logoutEntity = new HttpEntity<>("", headers);

        // 通过Gateway发送登出请求
        String logoutUrl = gatewayBaseUrl + "/user/logout";
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            logoutUrl, HttpMethod.POST, logoutEntity, String.class);

        // 验证登出响应
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertNotNull(logoutResponse.getBody());
        assertTrue(logoutResponse.getBody().contains("登出成功"));
        assertTrue(logoutResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, logoutResponse.getHeaders().getContentType());
    }

    @Test
    public void testUserWorkflow_LoginThenLogout() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. 执行登录
        HttpEntity<String> loginEntity = new HttpEntity<>("{\"username\":\"testuser\"}", headers);
        String loginUrl = gatewayBaseUrl + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, loginEntity, String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("登录成功"));

        // 2. 执行登出
        HttpEntity<String> logoutEntity = new HttpEntity<>("", headers);
        String logoutUrl = gatewayBaseUrl + "/user/logout";
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            logoutUrl, HttpMethod.POST, logoutEntity, String.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertTrue(logoutResponse.getBody().contains("登出成功"));
    }

    @Test
    public void testProductServiceFlow_ThroughGateway() {
        // 测试产品服务的完整流程
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. 获取产品列表
        String productsUrl = gatewayBaseUrl + "/product/list";
        ResponseEntity<String> productsResponse = restTemplate.getForEntity(productsUrl, String.class);

        assertEquals(HttpStatus.OK, productsResponse.getStatusCode());
        assertTrue(productsResponse.getBody().contains("products"));

        // 2. 获取特定产品
        String productUrl = gatewayBaseUrl + "/product/1";
        ResponseEntity<String> productResponse = restTemplate.getForEntity(productUrl, String.class);

        assertEquals(HttpStatus.OK, productResponse.getStatusCode());
        assertTrue(productResponse.getBody().contains("Product 1"));

        // 3. 创建新产品
        HttpEntity<String> createEntity = new HttpEntity<>("{\"name\":\"New Product\",\"price\":99.99}", headers);
        String createUrl = gatewayBaseUrl + "/product/create";
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createEntity, String.class);

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertTrue(createResponse.getBody().contains("created"));
    }

    @Test
    public void testGatewayHealthCheck() {
        // 测试Gateway健康检查端点
        String healthUrl = gatewayBaseUrl + "/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);

        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
    }

    @Test
    public void testConcurrentRequests_ThroughGateway() throws InterruptedException {
        // 测试并发请求处理
        int numberOfThreads = 10;
        Thread[] threads = new Thread[numberOfThreads];
        boolean[] results = new boolean[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>("{\"username\":\"user" + threadIndex + "\"}", headers);

                    String url = gatewayBaseUrl + "/user/login";
                    ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                    results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                         response.getBody().contains("登录成功");
                } catch (Exception e) {
                    results[threadIndex] = false;
                }
            });
        }

        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }

        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }

        // 验证所有请求都成功
        for (boolean result : results) {
            assertTrue(result, "并发请求应该都成功");
        }
    }

    @Test
    public void testRequestHeadersAndParametersForwarding() {
        // 测试请求头和参数的完整转发
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Custom-Header", "test-value");
        headers.set("Authorization", "Bearer test-token");

        HttpEntity<String> entity = new HttpEntity<>("{\"data\":\"test\"}", headers);

        String url = gatewayBaseUrl + "/user/info?userId=123&includeDetails=true";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        // 验证下游服务收到了正确的头和参数
        assertTrue(response.getBody().contains("X-Custom-Header"));
        assertTrue(response.getBody().contains("Authorization"));
        assertTrue(response.getBody().contains("userId=123"));
    }

    @Test
    public void testErrorHandlingAndFallback() {
        // 测试错误处理和fallback机制
        
        // 1. 测试服务不可用时的fallback
        userService.stop(); // 停止用户服务模拟不可用

        String url = gatewayBaseUrl + "/user/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 应该返回fallback响应
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("temporarily unavailable"));

        // 重新启动服务进行清理
        userService.start();
        setupUserServiceMocks();
    }

    @Test
    @EnabledIfSystemProperty(named = "e2e.test", matches = "true")
    public void testRealServicesIntegration() {
        // 这个测试需要真实的服务运行
        // 使用系统属性 -De2e.test=true 来启用
        String realGatewayUrl = "http://localhost:8080";
        
        ResponseEntity<String> response = restTemplate.getForEntity(
            realGatewayUrl + "/actuator/health", String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP"));
    }

    // Helper methods for setting up mock services

    private void setupUserServiceMocks() {
        WireMock.configureFor("localhost", userService.port());
        
        // Login endpoint
        userService.stubFor(post(urlEqualTo("/user/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"message\": \"登录成功\", \"token\": \"test-token\"}")));

        // Logout endpoint
        userService.stubFor(post(urlEqualTo("/user/logout"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"message\": \"登出成功\"}")));

        // Info endpoint with header and parameter forwarding
        userService.stubFor(post(urlPathEqualTo("/user/info"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"headers\": \"{{request.headers}}\", \"query\": \"{{request.query}}\"}")
                        .withTransformers("response-template")));
    }

    private void setupProductServiceMocks() {
        WireMock.configureFor("localhost", productService.port());
        
        // Product list endpoint
        productService.stubFor(get(urlEqualTo("/product/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"products\": [{\"id\": 1, \"name\": \"Product 1\"}, {\"id\": 2, \"name\": \"Product 2\"}]}")));

        // Single product endpoint
        productService.stubFor(get(urlPathMatching("/product/\\d+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"Product 1\", \"price\": 29.99}")));

        // Create product endpoint
        productService.stubFor(post(urlEqualTo("/product/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"message\": \"Product created\", \"id\": 3}")));
    }
}
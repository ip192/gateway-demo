package com.example.gateway;

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
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the custom info routing functionality.
 * Tests the complete routing flow through the gateway for /info endpoint.
 * 
 * These tests require all services (gateway, user-service, product-service) to be running.
 * Enable with system property: -De2e.test=true
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "e2e.test", matches = "true")
@TestPropertySource(properties = {
    "gateway.info-routing.enabled=true",
    "gateway.info-routing.prefixes.user=user-",
    "gateway.info-routing.prefixes.product=product-",
    "gateway.info-routing.services.user-service.url=http://localhost:8081",
    "gateway.info-routing.services.product-service.url=http://localhost:8082"
})
public class InfoRoutingIntegrationTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private String getGatewayUrl() {
        return "http://localhost:" + gatewayPort;
    }

    @Test
    public void testUserServiceRouting_ThroughGateway() {
        // 准备用户ID请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"id\":\"user-123\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway发送/info请求
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("user info for id: user-123"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testProductServiceRouting_ThroughGateway() {
        // 准备产品ID请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"id\":\"product-456\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway发送/info请求
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("product info for id: product-456"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testMissingIdParameter_ShouldReturnError() {
        // 准备无ID参数的请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway发送/info请求
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证错误响应
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Missing required parameter: id"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testInvalidIdPrefix_ShouldReturnError() {
        // 准备无效前缀的请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"id\":\"invalid-123\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway发送/info请求
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证错误响应
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Unknown id prefix"));
        assertTrue(response.getBody().contains("'user-' or 'product-'"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testEmptyRequestBody_ShouldReturnError() {
        // 准备空请求体
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway发送/info请求
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证错误响应
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Missing request body"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testInvalidJsonFormat_ShouldReturnError() {
        // 准备无效JSON格式的请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{invalid json}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway发送/info请求
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证错误响应
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Invalid JSON format"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testGetMethodNotAllowed_ShouldReturnError() {
        // 使用GET方法访问/info端点
        String url = getGatewayUrl() + "/info";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // 验证方法不允许的错误响应
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Method not allowed"));
        assertTrue(response.getBody().contains("Use POST for /info endpoint"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testExistingUserRoutes_ShouldNotBeAffected() {
        // 测试现有的用户服务路由不受影响
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 通过Gateway访问现有的用户登录端点
        String loginUrl = getGatewayUrl() + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, entity, String.class);

        // 验证现有路由正常工作
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("登录成功"));
        assertTrue(loginResponse.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, loginResponse.getHeaders().getContentType());
    }

    @Test
    public void testExistingProductRoutes_ShouldNotBeAffected() {
        // 测试现有的产品服务路由不受影响
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 通过Gateway访问现有的产品添加端点
        String addUrl = getGatewayUrl() + "/product/add";
        ResponseEntity<String> addResponse = restTemplate.exchange(
            addUrl, HttpMethod.POST, entity, String.class);

        // 验证现有路由正常工作
        assertEquals(HttpStatus.OK, addResponse.getStatusCode());
        assertNotNull(addResponse.getBody());
        assertTrue(addResponse.getBody().contains("产品添加成功"));
        assertTrue(addResponse.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, addResponse.getHeaders().getContentType());
    }

    @Test
    public void testConcurrentInfoRequests_ShouldHandleCorrectly() throws InterruptedException {
        // 测试并发/info请求处理
        int numberOfThreads = 5;
        Thread[] threads = new Thread[numberOfThreads];
        boolean[] results = new boolean[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            final String id = (threadIndex % 2 == 0) ? "user-" + threadIndex : "product-" + threadIndex;
            
            threads[i] = new Thread(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    String requestBody = "{\"id\":\"" + id + "\"}";
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                    String url = getGatewayUrl() + "/info";
                    ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                    results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                         response.getBody().contains("info for id: " + id) &&
                                         response.getBody().contains("success");
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
            assertTrue(result, "并发/info请求应该都成功");
        }
    }

    @Test
    public void testMixedWorkflow_InfoAndExistingRoutes() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. 测试用户info路由
        String userInfoBody = "{\"id\":\"user-workflow\"}";
        HttpEntity<String> userInfoEntity = new HttpEntity<>(userInfoBody, headers);
        String infoUrl = getGatewayUrl() + "/info";
        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
            infoUrl, HttpMethod.POST, userInfoEntity, String.class);

        assertEquals(HttpStatus.OK, userInfoResponse.getStatusCode());
        assertTrue(userInfoResponse.getBody().contains("user info for id: user-workflow"));

        // 2. 测试现有用户登录路由
        HttpEntity<String> loginEntity = new HttpEntity<>("{}", headers);
        String loginUrl = getGatewayUrl() + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, loginEntity, String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("登录成功"));

        // 3. 测试产品info路由
        String productInfoBody = "{\"id\":\"product-workflow\"}";
        HttpEntity<String> productInfoEntity = new HttpEntity<>(productInfoBody, headers);
        ResponseEntity<String> productInfoResponse = restTemplate.exchange(
            infoUrl, HttpMethod.POST, productInfoEntity, String.class);

        assertEquals(HttpStatus.OK, productInfoResponse.getStatusCode());
        assertTrue(productInfoResponse.getBody().contains("product info for id: product-workflow"));

        // 4. 测试现有产品查询路由
        HttpEntity<String> queryEntity = new HttpEntity<>("{}", headers);
        String queryUrl = getGatewayUrl() + "/product/query";
        ResponseEntity<String> queryResponse = restTemplate.exchange(
            queryUrl, HttpMethod.POST, queryEntity, String.class);

        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        assertTrue(queryResponse.getBody().contains("产品查询成功"));
    }
}
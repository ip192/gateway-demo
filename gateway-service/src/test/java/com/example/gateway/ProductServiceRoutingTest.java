package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 产品服务路由测试
 * 验证Gateway到产品服务的路由功能
 * 测试 /product/** 路径的路由功能，验证请求转发到 product-service
 * 需要同时启动Gateway服务和Product服务才能运行
 * 使用系统属性 -De2e.test=true 来启用这些测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnabledIfSystemProperty(named = "e2e.test", matches = "true")
public class ProductServiceRoutingTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    @Test
    public void testProductAddRouting_ThroughGateway() {
        // 准备产品添加请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> addEntity = new HttpEntity<>("{}", headers);

        // 通过Gateway路由发送产品添加请求
        String addUrl = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> addResponse = restTemplate.exchange(
            addUrl, HttpMethod.POST, addEntity, String.class);

        // 验证产品添加响应
        assertEquals(HttpStatus.OK, addResponse.getStatusCode());
        assertNotNull(addResponse.getBody());
        assertTrue(addResponse.getBody().contains("产品添加成功"));
        assertTrue(addResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, addResponse.getHeaders().getContentType());
    }

    @Test
    public void testProductQueryRouting_ThroughGateway() {
        // 准备产品查询请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> queryEntity = new HttpEntity<>("{}", headers);

        // 通过Gateway路由发送产品查询请求
        String queryUrl = GATEWAY_BASE_URL + "/product/query";
        ResponseEntity<String> queryResponse = restTemplate.exchange(
            queryUrl, HttpMethod.POST, queryEntity, String.class);

        // 验证产品查询响应
        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        assertNotNull(queryResponse.getBody());
        assertTrue(queryResponse.getBody().contains("产品查询成功"));
        assertTrue(queryResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, queryResponse.getHeaders().getContentType());
    }

    @Test
    public void testProductInfoRouting_ThroughGateway() {
        // 准备产品信息查询请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"id\": \"456\"}";
        HttpEntity<String> infoEntity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway路由发送产品信息查询请求
        String infoUrl = GATEWAY_BASE_URL + "/product/info";
        ResponseEntity<String> infoResponse = restTemplate.exchange(
            infoUrl, HttpMethod.POST, infoEntity, String.class);

        // 验证产品信息查询响应
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
        assertNotNull(infoResponse.getBody());
        assertTrue(infoResponse.getBody().contains("product info for id: 456"));
        assertTrue(infoResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, infoResponse.getHeaders().getContentType());
    }

    @Test
    public void testProductServicePathMatching() {
        // 测试 /product/** 路径匹配功能
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 测试不同的产品服务路径
        String[] productPaths = {"/product/add", "/product/query", "/product/info"};
        
        for (String path : productPaths) {
            String url = GATEWAY_BASE_URL + path;
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            // 验证Gateway正确路由到产品服务
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("success"));
        }
    }

    @Test
    public void testGatewayRouting_ProductService() {
        // 测试Gateway路由到产品服务的集成
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 通过Gateway路由调用产品服务
        String url = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证Gateway路由正确调用了产品服务
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证响应格式符合ApiResponse结构
        assertTrue(response.getBody().contains("message"));
        assertTrue(response.getBody().contains("status"));
        assertTrue(response.getBody().contains("产品添加成功"));
        assertTrue(response.getBody().contains("success"));
    }

    @Test
    public void testProductServiceHealthCheck() {
        // 测试产品服务健康检查端点
        String healthUrl = "http://localhost:8082/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);

        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
    }

    @Test
    public void testProductWorkflow_AddThenQuery() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. 执行产品添加
        HttpEntity<String> addEntity = new HttpEntity<>("{}", headers);
        String addUrl = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> addResponse = restTemplate.exchange(
            addUrl, HttpMethod.POST, addEntity, String.class);

        assertEquals(HttpStatus.OK, addResponse.getStatusCode());
        assertTrue(addResponse.getBody().contains("产品添加成功"));

        // 2. 执行产品查询
        HttpEntity<String> queryEntity = new HttpEntity<>("{}", headers);
        String queryUrl = GATEWAY_BASE_URL + "/product/query";
        ResponseEntity<String> queryResponse = restTemplate.exchange(
            queryUrl, HttpMethod.POST, queryEntity, String.class);

        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        assertTrue(queryResponse.getBody().contains("产品查询成功"));
    }

    @Test
    public void testConcurrentProductRequests_ThroughGateway() throws InterruptedException {
        // 测试并发产品请求处理
        int numberOfThreads = 5;
        Thread[] threads = new Thread[numberOfThreads];
        boolean[] results = new boolean[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>("{}", headers);

                    String url = GATEWAY_BASE_URL + "/product/add";
                    ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                    results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                         response.getBody().contains("产品添加成功");
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
            assertTrue(result, "并发产品请求应该都成功");
        }
    }

    @Test
    public void testProductServiceRequestForwarding() {
        // 验证请求转发到 product-service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 通过Gateway发送请求
        String gatewayUrl = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> gatewayResponse = restTemplate.exchange(
            gatewayUrl, HttpMethod.POST, entity, String.class);

        // 直接调用产品服务进行对比
        String directUrl = "http://localhost:8082/product/add";
        ResponseEntity<String> directResponse = restTemplate.exchange(
            directUrl, HttpMethod.POST, entity, String.class);

        // 验证Gateway转发的响应与直接调用产品服务的响应一致
        assertEquals(directResponse.getStatusCode(), gatewayResponse.getStatusCode());
        assertEquals(directResponse.getBody(), gatewayResponse.getBody());
    }
}
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
 * 产品服务端到端集成测试
 * 验证Gateway到产品服务的完整调用链
 * 需要同时启动Gateway服务和Product服务才能运行
 * 使用系统属性 -De2e.test=true 来启用这些测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnabledIfSystemProperty(named = "e2e.test", matches = "true")
public class ProductServiceEndToEndTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    @Test
    public void testProductAddFlow_ThroughGateway() {
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
    public void testProductQueryFlow_ThroughGateway() {
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
    public void testCircuitBreakerErrorHandling_ProductServiceDown() {
        // 这个测试需要产品服务关闭时运行，用于测试Circuit Breaker的错误处理
        // 在实际测试中，可以通过配置错误的URL或使用WireMock来模拟服务不可用
        
        // 注意：这个测试在正常的端到端测试中会被跳过，
        // 因为我们期望产品服务是运行的
        // 可以通过系统属性来控制是否运行此测试
        if (System.getProperty("test.service.down", "false").equals("true")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>("{}", headers);

            String url = GATEWAY_BASE_URL + "/product/add";
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            // 当产品服务不可用时，应该返回fallback响应
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().contains("服务暂时不可用"));
        }
    }
}
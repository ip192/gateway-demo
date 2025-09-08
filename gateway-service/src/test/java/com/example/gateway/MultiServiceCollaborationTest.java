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
 * 多服务协作测试
 * 测试Gateway同时调用用户服务和产品服务的场景
 * 验证服务间通信的正确性
 * 需要同时启动Gateway服务、User服务和Product服务才能运行
 * 使用系统属性 -De2e.test=true 来启用这些测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnabledIfSystemProperty(named = "e2e.test", matches = "true")
public class MultiServiceCollaborationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    @Test
    public void testUserAndProductServices_BothAvailable() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 1. 测试用户服务登录
        String userLoginUrl = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> userLoginResponse = restTemplate.exchange(
            userLoginUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, userLoginResponse.getStatusCode());
        assertTrue(userLoginResponse.getBody().contains("登录成功"));
        assertTrue(userLoginResponse.getBody().contains("success"));

        // 2. 测试产品服务添加
        String productAddUrl = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> productAddResponse = restTemplate.exchange(
            productAddUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, productAddResponse.getStatusCode());
        assertTrue(productAddResponse.getBody().contains("产品添加成功"));
        assertTrue(productAddResponse.getBody().contains("success"));

        // 3. 测试产品服务查询
        String productQueryUrl = GATEWAY_BASE_URL + "/product/query";
        ResponseEntity<String> productQueryResponse = restTemplate.exchange(
            productQueryUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, productQueryResponse.getStatusCode());
        assertTrue(productQueryResponse.getBody().contains("产品查询成功"));
        assertTrue(productQueryResponse.getBody().contains("success"));

        // 4. 测试用户服务登出
        String userLogoutUrl = GATEWAY_BASE_URL + "/user/logout";
        ResponseEntity<String> userLogoutResponse = restTemplate.exchange(
            userLogoutUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, userLogoutResponse.getStatusCode());
        assertTrue(userLogoutResponse.getBody().contains("登出成功"));
        assertTrue(userLogoutResponse.getBody().contains("success"));
    }

    @Test
    public void testCompleteBusinessWorkflow_UserLoginProductOperationsLogout() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 模拟完整的业务流程：用户登录 -> 产品操作 -> 用户登出
        
        // 步骤1: 用户登录
        String loginUrl = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("登录成功"));
        System.out.println("用户登录成功: " + loginResponse.getBody());

        // 步骤2: 添加产品
        String addProductUrl = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> addProductResponse = restTemplate.exchange(
            addProductUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, addProductResponse.getStatusCode());
        assertTrue(addProductResponse.getBody().contains("产品添加成功"));
        System.out.println("产品添加成功: " + addProductResponse.getBody());

        // 步骤3: 查询产品
        String queryProductUrl = GATEWAY_BASE_URL + "/product/query";
        ResponseEntity<String> queryProductResponse = restTemplate.exchange(
            queryProductUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, queryProductResponse.getStatusCode());
        assertTrue(queryProductResponse.getBody().contains("产品查询成功"));
        System.out.println("产品查询成功: " + queryProductResponse.getBody());

        // 步骤4: 用户登出
        String logoutUrl = GATEWAY_BASE_URL + "/user/logout";
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            logoutUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertTrue(logoutResponse.getBody().contains("登出成功"));
        System.out.println("用户登出成功: " + logoutResponse.getBody());
    }

    @Test
    public void testConcurrentMultiServiceRequests() throws InterruptedException {
        // 测试并发调用多个服务
        int numberOfThreads = 4;
        Thread[] threads = new Thread[numberOfThreads];
        boolean[] results = new boolean[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>("{}", headers);

                    // 每个线程交替调用用户服务和产品服务
                    if (threadIndex % 2 == 0) {
                        // 调用用户服务
                        String url = GATEWAY_BASE_URL + "/user/login";
                        ResponseEntity<String> response = restTemplate.exchange(
                            url, HttpMethod.POST, entity, String.class);
                        results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                             response.getBody().contains("登录成功");
                    } else {
                        // 调用产品服务
                        String url = GATEWAY_BASE_URL + "/product/add";
                        ResponseEntity<String> response = restTemplate.exchange(
                            url, HttpMethod.POST, entity, String.class);
                        results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                             response.getBody().contains("产品添加成功");
                    }
                } catch (Exception e) {
                    results[threadIndex] = false;
                    e.printStackTrace();
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
        for (int i = 0; i < results.length; i++) {
            assertTrue(results[i], "并发请求 " + i + " 应该成功");
        }
    }

    @Test
    public void testServiceCommunicationReliability() {
        // 测试服务间通信的可靠性
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 连续多次调用不同服务，验证通信稳定性
        for (int i = 0; i < 10; i++) {
            // 调用用户服务
            String userUrl = GATEWAY_BASE_URL + "/user/login";
            ResponseEntity<String> userResponse = restTemplate.exchange(
                userUrl, HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.OK, userResponse.getStatusCode(), 
                "第 " + (i + 1) + " 次用户服务调用失败");
            assertTrue(userResponse.getBody().contains("登录成功"));

            // 调用产品服务
            String productUrl = GATEWAY_BASE_URL + "/product/query";
            ResponseEntity<String> productResponse = restTemplate.exchange(
                productUrl, HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.OK, productResponse.getStatusCode(), 
                "第 " + (i + 1) + " 次产品服务调用失败");
            assertTrue(productResponse.getBody().contains("产品查询成功"));

            // 短暂延迟以模拟真实使用场景
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Test
    public void testAllServicesHealthCheck() {
        // 验证所有服务的健康状态
        
        // 1. Gateway服务健康检查
        String gatewayHealthUrl = GATEWAY_BASE_URL + "/actuator/health";
        ResponseEntity<String> gatewayHealth = restTemplate.getForEntity(gatewayHealthUrl, String.class);
        assertEquals(HttpStatus.OK, gatewayHealth.getStatusCode());
        assertTrue(gatewayHealth.getBody().contains("UP"));

        // 2. 用户服务健康检查
        String userHealthUrl = "http://localhost:8081/actuator/health";
        ResponseEntity<String> userHealth = restTemplate.getForEntity(userHealthUrl, String.class);
        assertEquals(HttpStatus.OK, userHealth.getStatusCode());
        assertTrue(userHealth.getBody().contains("UP"));

        // 3. 产品服务健康检查
        String productHealthUrl = "http://localhost:8082/actuator/health";
        ResponseEntity<String> productHealth = restTemplate.getForEntity(productHealthUrl, String.class);
        assertEquals(HttpStatus.OK, productHealth.getStatusCode());
        assertTrue(productHealth.getBody().contains("UP"));
    }

    @Test
    public void testGatewayRoutingLoadBalancing() {
        // 测试Gateway路由的连接稳定性和负载处理
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 快速连续调用，验证Gateway路由连接池和负载处理
        for (int i = 0; i < 20; i++) {
            String url = (i % 2 == 0) ? 
                GATEWAY_BASE_URL + "/user/login" : 
                GATEWAY_BASE_URL + "/product/add";

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "第 " + (i + 1) + " 次Gateway路由调用失败");
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("success"));
        }
    }

    @Test
    public void testServiceResponseConsistency() {
        // 测试服务响应格式的一致性
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 调用用户服务
        String userUrl = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> userResponse = restTemplate.exchange(
            userUrl, HttpMethod.POST, entity, String.class);

        // 调用产品服务
        String productUrl = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> productResponse = restTemplate.exchange(
            productUrl, HttpMethod.POST, entity, String.class);

        // 验证两个服务都返回相同的响应格式
        assertEquals(HttpStatus.OK, userResponse.getStatusCode());
        assertEquals(HttpStatus.OK, productResponse.getStatusCode());

        // 验证响应都包含ApiResponse的标准字段
        assertTrue(userResponse.getBody().contains("message"));
        assertTrue(userResponse.getBody().contains("status"));
        assertTrue(productResponse.getBody().contains("message"));
        assertTrue(productResponse.getBody().contains("status"));

        // 验证响应都是JSON格式
        assertEquals(MediaType.APPLICATION_JSON, userResponse.getHeaders().getContentType());
        assertEquals(MediaType.APPLICATION_JSON, productResponse.getHeaders().getContentType());
    }
}
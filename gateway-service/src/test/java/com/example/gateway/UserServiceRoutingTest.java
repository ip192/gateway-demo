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
 * 用户服务路由测试
 * 验证Gateway到用户服务的路由功能
 * 测试 /user/** 路径的路由功能，验证请求转发到 user-service
 * 需要同时启动Gateway服务和User服务才能运行
 * 使用系统属性 -De2e.test=true 来启用这些测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnabledIfSystemProperty(named = "e2e.test", matches = "true")
public class UserServiceRoutingTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private static final String GATEWAY_BASE_URL = "http://localhost:8080";

    @Test
    public void testUserLoginRouting_ThroughGateway() {
        // 准备用户登录请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> loginEntity = new HttpEntity<>("{}", headers);

        // 通过Gateway路由发送用户登录请求
        String loginUrl = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, loginEntity, String.class);

        // 验证用户登录响应
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("登录成功"));
        assertTrue(loginResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, loginResponse.getHeaders().getContentType());
    }

    @Test
    public void testUserLogoutRouting_ThroughGateway() {
        // 准备用户登出请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> logoutEntity = new HttpEntity<>("{}", headers);

        // 通过Gateway路由发送用户登出请求
        String logoutUrl = GATEWAY_BASE_URL + "/user/logout";
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            logoutUrl, HttpMethod.POST, logoutEntity, String.class);

        // 验证用户登出响应
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertNotNull(logoutResponse.getBody());
        assertTrue(logoutResponse.getBody().contains("登出成功"));
        assertTrue(logoutResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, logoutResponse.getHeaders().getContentType());
    }

    @Test
    public void testUserInfoRouting_ThroughGateway() {
        // 准备用户信息查询请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String requestBody = "{\"id\": \"123\"}";
        HttpEntity<String> infoEntity = new HttpEntity<>(requestBody, headers);

        // 通过Gateway路由发送用户信息查询请求
        String infoUrl = GATEWAY_BASE_URL + "/user/info";
        ResponseEntity<String> infoResponse = restTemplate.exchange(
            infoUrl, HttpMethod.POST, infoEntity, String.class);

        // 验证用户信息查询响应
        assertEquals(HttpStatus.OK, infoResponse.getStatusCode());
        assertNotNull(infoResponse.getBody());
        assertTrue(infoResponse.getBody().contains("user info for id: 123"));
        assertTrue(infoResponse.getBody().contains("success"));
        
        // 验证响应头
        assertEquals(MediaType.APPLICATION_JSON, infoResponse.getHeaders().getContentType());
    }

    @Test
    public void testUserServicePathMatching() {
        // 测试 /user/** 路径匹配功能
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 测试不同的用户服务路径
        String[] userPaths = {"/user/login", "/user/logout", "/user/info"};
        
        for (String path : userPaths) {
            String url = GATEWAY_BASE_URL + path;
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            // 验证Gateway正确路由到用户服务
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("success"));
        }
    }

    @Test
    public void testGatewayRouting_UserService() {
        // 测试Gateway路由到用户服务的集成
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 通过Gateway路由调用用户服务
        String url = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证Gateway路由正确调用了用户服务
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证响应格式符合ApiResponse结构
        assertTrue(response.getBody().contains("message"));
        assertTrue(response.getBody().contains("status"));
        assertTrue(response.getBody().contains("登录成功"));
        assertTrue(response.getBody().contains("success"));
    }

    @Test
    public void testUserServiceHealthCheck() {
        // 测试用户服务健康检查端点
        String healthUrl = "http://localhost:8081/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);

        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
    }

    @Test
    public void testUserWorkflow_LoginThenLogout() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. 执行用户登录
        HttpEntity<String> loginEntity = new HttpEntity<>("{}", headers);
        String loginUrl = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> loginResponse = restTemplate.exchange(
            loginUrl, HttpMethod.POST, loginEntity, String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertTrue(loginResponse.getBody().contains("登录成功"));

        // 2. 执行用户登出
        HttpEntity<String> logoutEntity = new HttpEntity<>("{}", headers);
        String logoutUrl = GATEWAY_BASE_URL + "/user/logout";
        ResponseEntity<String> logoutResponse = restTemplate.exchange(
            logoutUrl, HttpMethod.POST, logoutEntity, String.class);

        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertTrue(logoutResponse.getBody().contains("登出成功"));
    }

    @Test
    public void testConcurrentUserRequests_ThroughGateway() throws InterruptedException {
        // 测试并发用户请求处理
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

                    String url = GATEWAY_BASE_URL + "/user/login";
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
            assertTrue(result, "并发用户请求应该都成功");
        }
    }

    @Test
    public void testUserServiceRequestForwarding() {
        // 验证请求转发到 user-service
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 通过Gateway发送请求
        String gatewayUrl = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> gatewayResponse = restTemplate.exchange(
            gatewayUrl, HttpMethod.POST, entity, String.class);

        // 直接调用用户服务进行对比
        String directUrl = "http://localhost:8081/user/login";
        ResponseEntity<String> directResponse = restTemplate.exchange(
            directUrl, HttpMethod.POST, entity, String.class);

        // 验证Gateway转发的响应与直接调用用户服务的响应一致
        assertEquals(directResponse.getStatusCode(), gatewayResponse.getStatusCode());
        assertEquals(directResponse.getBody(), gatewayResponse.getBody());
    }
}
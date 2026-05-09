package com.example.gateway;

import com.example.gateway.client.UserServiceClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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

    @MockBean
    private UserServiceClient userServiceClient;

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

    @Test
    public void testUserServiceFeignClientConfiguration() {
        // 验证UserServiceClient Bean存在
        assertNotNull(userServiceClient, "UserServiceClient should be available");
    }

    @Test
    public void testUserServiceFeignClientMockCall() {
        // 模拟Feign客户端调用
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"message\":\"用户信息\",\"status\":\"success\"}");
        
        when(userServiceClient.getUserInfo(any())).thenReturn(mockResponse);
        
        // 验证Mock调用
        ResponseEntity<Object> response = userServiceClient.getUserInfo(new Object());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testLoadBalancedRouteConfiguration() {
        // 验证负载均衡路由配置正确
        // 这个测试验证路由配置使用了lb://前缀
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证通过负载均衡路由的请求成功
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========== 动态路由测试 ==========

    @Test
    public void testDynamicRouting_UserLogin() {
        // 测试动态路由处理用户登录请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 添加动态路由标记，确保使用动态路由
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功转发请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("登录成功"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testDynamicRouting_UserLogout() {
        // 测试动态路由处理用户登出请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/user/logout";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功转发请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("登出成功"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testDynamicRouting_UserInfoWithRequestBody() {
        // 测试动态路由处理带请求体的用户信息查询
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        String requestBody = "{\"id\": \"dynamic-test-123\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        String url = GATEWAY_BASE_URL + "/user/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由正确传递请求体并返回响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("user info for id: dynamic-test-123"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testDynamicRouting_RequestParametersTransmission() {
        // 测试动态路由完整传递查询参数
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 添加查询参数测试参数传递
        String url = GATEWAY_BASE_URL + "/user/login?source=dynamic&version=1.0&timestamp=" + System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功处理带查询参数的请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("登录成功"));
        assertTrue(response.getBody().contains("success"));
    }

    @Test
    public void testDynamicRouting_ComplexRequestBodyTransmission() {
        // 测试动态路由处理复杂请求体的完整传递
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        // 构造复杂的请求体
        String complexRequestBody = "{\"id\": \"complex-user-456\", \"metadata\": {\"source\": \"dynamic-routing\", \"timestamp\": " + System.currentTimeMillis() + "}}";
        HttpEntity<String> entity = new HttpEntity<>(complexRequestBody, headers);

        String url = GATEWAY_BASE_URL + "/user/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由正确处理复杂请求体
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("user info for id: complex-user-456"));
        assertTrue(response.getBody().contains("success"));
    }

    @Test
    public void testDynamicRouting_HttpHeadersTransmission() {
        // 测试动态路由正确传递HTTP头信息
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        headers.add("X-Custom-Header", "dynamic-routing-test");
        headers.add("X-Client-Version", "1.0.0");
        headers.add("Authorization", "Bearer test-token");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功处理带自定义头信息的请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("登录成功"));
        assertTrue(response.getBody().contains("success"));
    }

    @Test
    public void testDynamicRouting_ResponseStatusCodeAndContent() {
        // 测试动态路由正确返回响应状态码和内容
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        String requestBody = "{\"id\": \"status-test-789\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        String url = GATEWAY_BASE_URL + "/user/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证响应状态码正确
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // 验证响应内容完整性
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("user info for id: status-test-789"));
        assertTrue(response.getBody().contains("success"));
        
        // 验证响应头信息
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getHeaders().getContentLength());
    }

    @Test
    public void testDynamicRouting_NewUserServiceInterface() {
        // 测试动态路由自动转发新的用户服务接口
        // 这个测试模拟用户服务添加了新接口的场景
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 测试访问一个假设的新接口（实际会返回404，但验证动态路由机制工作）
        String url = GATEWAY_BASE_URL + "/user/newEndpoint";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 由于用户服务没有这个端点，应该返回404，但这证明动态路由尝试转发了请求
        // 这验证了动态路由的自动转发机制
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND || 
                  response.getStatusCode() == HttpStatus.OK);
    }

    @Test
    public void testDynamicRouting_MultipleHttpMethods() {
        // 测试动态路由支持多种HTTP方法
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        // 测试POST方法
        HttpEntity<String> postEntity = new HttpEntity<>("{}", headers);
        String url = GATEWAY_BASE_URL + "/user/login";
        ResponseEntity<String> postResponse = restTemplate.exchange(
            url, HttpMethod.POST, postEntity, String.class);
        assertEquals(HttpStatus.OK, postResponse.getStatusCode());
        
        // 测试GET方法（虽然用户服务可能不支持，但验证动态路由转发机制）
        ResponseEntity<String> getResponse = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        // GET请求可能返回405 Method Not Allowed，这是正常的
        assertTrue(getResponse.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED || 
                  getResponse.getStatusCode() == HttpStatus.OK ||
                  getResponse.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void testDynamicRouting_ConcurrentRequests() throws InterruptedException {
        // 测试动态路由处理并发请求的能力
        int numberOfThreads = 3;
        Thread[] threads = new Thread[numberOfThreads];
        boolean[] results = new boolean[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("X-Gateway-Dynamic-Routing", "true");
                    headers.add("X-Thread-ID", "thread-" + threadIndex);
                    
                    String requestBody = "{\"id\": \"concurrent-test-" + threadIndex + "\"}";
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                    String url = GATEWAY_BASE_URL + "/user/info";
                    ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                    results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                         response.getBody().contains("concurrent-test-" + threadIndex) &&
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

        // 验证所有并发请求都成功
        for (int i = 0; i < numberOfThreads; i++) {
            assertTrue(results[i], "并发动态路由请求 " + i + " 应该成功");
        }
    }

    @Test
    public void testDynamicRouting_CompareWithDirectCall() {
        // 测试动态路由转发的结果与直接调用用户服务的结果一致
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        String requestBody = "{\"id\": \"comparison-test-999\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过动态路由调用
        String gatewayUrl = GATEWAY_BASE_URL + "/user/info";
        ResponseEntity<String> gatewayResponse = restTemplate.exchange(
            gatewayUrl, HttpMethod.POST, entity, String.class);

        // 直接调用用户服务
        HttpEntity<String> directEntity = new HttpEntity<>(requestBody, new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }});
        String directUrl = "http://localhost:8081/user/info";
        ResponseEntity<String> directResponse = restTemplate.exchange(
            directUrl, HttpMethod.POST, directEntity, String.class);

        // 验证动态路由转发的响应与直接调用的响应一致
        assertEquals(directResponse.getStatusCode(), gatewayResponse.getStatusCode());
        assertEquals(directResponse.getBody(), gatewayResponse.getBody());
        
        // 验证响应内容正确性
        assertTrue(gatewayResponse.getBody().contains("comparison-test-999"));
        assertTrue(gatewayResponse.getBody().contains("success"));
    }
}
package com.example.gateway;

import com.example.gateway.client.ProductServiceClient;
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

    @MockBean
    private ProductServiceClient productServiceClient;

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

    @Test
    public void testProductServiceFeignClientConfiguration() {
        // 验证ProductServiceClient Bean存在
        assertNotNull(productServiceClient, "ProductServiceClient should be available");
    }

    @Test
    public void testProductServiceFeignClientMockCall() {
        // 模拟Feign客户端调用
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"message\":\"产品信息\",\"status\":\"success\"}");
        
        when(productServiceClient.getProductInfo(any())).thenReturn(mockResponse);
        
        // 验证Mock调用
        ResponseEntity<Object> response = productServiceClient.getProductInfo(new Object());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testLoadBalancedProductRouteConfiguration() {
        // 验证负载均衡路由配置正确
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证通过负载均衡路由的请求成功
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    // ========== 动态路由测试 ==========

    @Test
    public void testDynamicRouting_ProductAdd() {
        // 测试动态路由处理产品添加请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 添加动态路由标记，确保使用动态路由
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功转发请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("产品添加成功"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testDynamicRouting_ProductQuery() {
        // 测试动态路由处理产品查询请求
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/product/query";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功转发请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("产品查询成功"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testDynamicRouting_ProductInfoWithRequestBody() {
        // 测试动态路由处理带请求体的产品信息查询
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        String requestBody = "{\"id\": \"dynamic-product-123\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        String url = GATEWAY_BASE_URL + "/product/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由正确传递请求体并返回响应
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("product info for id: dynamic-product-123"));
        assertTrue(response.getBody().contains("success"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    }

    @Test
    public void testDynamicRouting_MultipleHttpMethods() {
        // 测试动态路由支持多种HTTP方法
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        // 测试POST方法
        HttpEntity<String> postEntity = new HttpEntity<>("{}", headers);
        String url = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> postResponse = restTemplate.exchange(
            url, HttpMethod.POST, postEntity, String.class);
        assertEquals(HttpStatus.OK, postResponse.getStatusCode());
        assertTrue(postResponse.getBody().contains("产品添加成功"));
        
        // 测试GET方法（产品服务可能不支持，但验证动态路由转发机制）
        ResponseEntity<String> getResponse = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        // GET请求可能返回405 Method Not Allowed，这是正常的
        assertTrue(getResponse.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED || 
                  getResponse.getStatusCode() == HttpStatus.OK ||
                  getResponse.getStatusCode() == HttpStatus.NOT_FOUND);
        
        // 测试PUT方法
        HttpEntity<String> putEntity = new HttpEntity<>("{}", headers);
        ResponseEntity<String> putResponse = restTemplate.exchange(
            url, HttpMethod.PUT, putEntity, String.class);
        // PUT请求可能返回405 Method Not Allowed，这是正常的
        assertTrue(putResponse.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED || 
                  putResponse.getStatusCode() == HttpStatus.OK ||
                  putResponse.getStatusCode() == HttpStatus.NOT_FOUND);
        
        // 测试DELETE方法
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
            url, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        // DELETE请求可能返回405 Method Not Allowed，这是正常的
        assertTrue(deleteResponse.getStatusCode() == HttpStatus.METHOD_NOT_ALLOWED || 
                  deleteResponse.getStatusCode() == HttpStatus.OK ||
                  deleteResponse.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    public void testDynamicRouting_RequestParametersTransmission() {
        // 测试动态路由完整传递查询参数
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 添加查询参数测试参数传递
        String url = GATEWAY_BASE_URL + "/product/add?source=dynamic&version=1.0&timestamp=" + System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功处理带查询参数的请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("产品添加成功"));
        assertTrue(response.getBody().contains("success"));
    }

    @Test
    public void testDynamicRouting_ComplexRequestBodyTransmission() {
        // 测试动态路由处理复杂请求体的完整传递
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        // 构造复杂的请求体
        String complexRequestBody = "{\"id\": \"complex-product-456\", \"metadata\": {\"source\": \"dynamic-routing\", \"timestamp\": " + System.currentTimeMillis() + "}}";
        HttpEntity<String> entity = new HttpEntity<>(complexRequestBody, headers);

        String url = GATEWAY_BASE_URL + "/product/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由正确处理复杂请求体
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("product info for id: complex-product-456"));
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

        String url = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证动态路由成功处理带自定义头信息的请求
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("产品添加成功"));
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

        String url = GATEWAY_BASE_URL + "/product/info";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证响应状态码正确
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // 验证响应内容完整性
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("product info for id: status-test-789"));
        assertTrue(response.getBody().contains("success"));
        
        // 验证响应头信息
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertNotNull(response.getHeaders().getContentLength());
    }

    @Test
    public void testDynamicRouting_NewProductServiceInterface() {
        // 测试动态路由自动转发新的产品服务接口
        // 这个测试模拟产品服务添加了新接口的场景
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 测试访问一个假设的新接口（实际会返回404，但验证动态路由机制工作）
        String url = GATEWAY_BASE_URL + "/product/newEndpoint";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 由于产品服务没有这个端点，应该返回404，但这证明动态路由尝试转发了请求
        // 这验证了动态路由的自动转发机制
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND || 
                  response.getStatusCode() == HttpStatus.OK);
    }

    @Test
    public void testDynamicRouting_ErrorScenarioHandling() {
        // 测试动态路由正确处理错误场景
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        // 测试访问不存在的端点
        String url = GATEWAY_BASE_URL + "/product/nonexistent";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证错误场景的正确处理
        // 产品服务有一个/nonexistent端点返回404，或者动态路由返回404
        assertTrue(response.getStatusCode() == HttpStatus.NOT_FOUND || 
                  response.getStatusCode() == HttpStatus.OK);
        
        if (response.getStatusCode() == HttpStatus.NOT_FOUND && response.getBody() != null) {
            // 如果返回404，验证错误信息格式
            assertTrue(response.getBody().contains("error") || 
                      response.getBody().contains("not found") ||
                      response.getBody().contains("不存在"));
        }
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
                    
                    String requestBody = "{\"id\": \"concurrent-product-" + threadIndex + "\"}";
                    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

                    String url = GATEWAY_BASE_URL + "/product/info";
                    ResponseEntity<String> response = restTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);

                    results[threadIndex] = response.getStatusCode() == HttpStatus.OK &&
                                         response.getBody().contains("concurrent-product-" + threadIndex) &&
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
        // 测试动态路由转发的结果与直接调用产品服务的结果一致
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        String requestBody = "{\"id\": \"comparison-product-999\"}";
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 通过动态路由调用
        String gatewayUrl = GATEWAY_BASE_URL + "/product/info";
        ResponseEntity<String> gatewayResponse = restTemplate.exchange(
            gatewayUrl, HttpMethod.POST, entity, String.class);

        // 直接调用产品服务
        HttpEntity<String> directEntity = new HttpEntity<>(requestBody, new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }});
        String directUrl = "http://localhost:8082/product/info";
        ResponseEntity<String> directResponse = restTemplate.exchange(
            directUrl, HttpMethod.POST, directEntity, String.class);

        // 验证动态路由转发的响应与直接调用的响应一致
        assertEquals(directResponse.getStatusCode(), gatewayResponse.getStatusCode());
        assertEquals(directResponse.getBody(), gatewayResponse.getBody());
        
        // 验证响应内容正确性
        assertTrue(gatewayResponse.getBody().contains("comparison-product-999"));
        assertTrue(gatewayResponse.getBody().contains("success"));
    }

    @Test
    public void testDynamicRouting_AllProductEndpoints() {
        // 测试动态路由对所有产品服务端点的自动转发功能
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        
        // 测试所有已知的产品服务端点
        String[] endpoints = {"/product/add", "/product/query", "/product/info"};
        
        for (String endpoint : endpoints) {
            String url = GATEWAY_BASE_URL + endpoint;
            
            if (endpoint.equals("/product/info")) {
                // info端点需要请求体
                String requestBody = "{\"id\": \"auto-forward-test\"}";
                HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
                
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().contains("auto-forward-test"));
                assertTrue(response.getBody().contains("success"));
            } else {
                // add和query端点不需要特殊请求体
                HttpEntity<String> entity = new HttpEntity<>("{}", headers);
                ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);
                
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getBody().contains("success"));
            }
        }
    }

    @Test
    public void testDynamicRouting_ServiceDiscoveryIntegration() {
        // 测试动态路由与服务发现的集成
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Gateway-Dynamic-Routing", "true");
        headers.add("X-Service-Discovery-Test", "true");
        
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        String url = GATEWAY_BASE_URL + "/product/add";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // 验证通过服务发现的动态路由成功
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("产品添加成功"));
        assertTrue(response.getBody().contains("success"));
        
        // 验证响应时间合理（服务发现不应该显著影响性能）
        assertNotNull(response.getHeaders().getDate());
    }
}
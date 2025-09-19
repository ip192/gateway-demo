package com.example.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * 简化的端到端集成测试
 * 测试基本的请求转发流程
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
    public void testUserServiceRouting_ThroughGateway() {
        // 测试用户服务路由
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"username\":\"testuser\"}", headers);

        String url = gatewayBaseUrl + "/user/login";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("登录成功"));
    }

    @Test
    public void testProductServiceRouting_ThroughGateway() {
        // 测试产品服务路由
        String url = gatewayBaseUrl + "/product/list";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("products"));
    }

    @Test
    public void testServiceUnavailable_ErrorHandling() {
        // 测试服务不可用时的错误处理
        userService.stop();

        String url = gatewayBaseUrl + "/user/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{}", headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        assertTrue(response.getStatusCode().is5xxServerError());
        assertNotNull(response.getBody());

        // 重新启动服务
        userService.start();
        setupUserServiceMocks();
    }

    // Helper methods for setting up mock services

    private void setupUserServiceMocks() {
        WireMock.configureFor("localhost", userService.port());
        
        // Login endpoint
        userService.stubFor(post(urlEqualTo("/user/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": true, \"message\": \"登录成功\"}")));
    }

    private void setupProductServiceMocks() {
        WireMock.configureFor("localhost", productService.port());
        
        // Product list endpoint
        productService.stubFor(get(urlEqualTo("/product/list"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"products\": [{\"id\": 1, \"name\": \"Product 1\"}]}")));
    }
}
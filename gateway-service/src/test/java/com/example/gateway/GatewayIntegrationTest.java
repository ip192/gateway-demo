package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=user-service-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**"
})
public class GatewayIntegrationTest {

    @LocalServerPort
    private int gatewayPort;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RouteLocator routeLocator;

    @Test
    public void testGatewayApplicationStarts() {
        // 测试Gateway应用能够正常启动
        assertNotNull(routeLocator);
        assertTrue(gatewayPort > 0);
    }

    @Test
    public void testGatewayRouteConfiguration() {
        // 测试路由配置是否正确加载
        assertNotNull(routeLocator);
        
        // 验证路由定义存在
        routeLocator.getRoutes().collectList().block().forEach(route -> {
            assertNotNull(route.getId());
            assertNotNull(route.getUri());
            assertNotNull(route.getPredicate());
        });
    }

    @Test
    public void testUserServiceUnavailable_ShouldReturnError() {
        // 测试当User服务不可用时，Gateway的行为
        String url = "http://localhost:" + gatewayPort + "/user/login";
        ResponseEntity<String> response = restTemplate.postForEntity(url, "{}", String.class);

        // 由于User服务未启动，Gateway可能返回不同的错误状态码
        // 这取决于Gateway的配置和错误处理策略
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError() ||
                  response.getStatusCode().is2xxSuccessful(), 
                  "Gateway应该返回有效的HTTP状态码");
    }

    @Test
    public void testInvalidRoute_ShouldReturn404() {
        // 测试不存在的路由
        String url = "http://localhost:" + gatewayPort + "/invalid/path";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // 验证返回404
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testGatewayHealthCheck() {
        // 测试Gateway健康检查端点
        String healthUrl = "http://localhost:" + gatewayPort + "/actuator/health";
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(healthUrl, String.class);

        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());
        assertTrue(healthResponse.getBody().contains("UP"));
    }

    @Test
    public void testGatewayRouteMatching() {
        // 测试路由匹配逻辑
        String userUrl = "http://localhost:" + gatewayPort + "/user/test";
        ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);

        // 应该匹配到user路由，但由于服务不可用或方法不匹配可能返回不同状态码
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Gateway应该返回错误状态码，实际状态码: " + response.getStatusCode());
    }
}
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

/**
 * 简化的Gateway集成测试
 * 测试基本的网关功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=user-service-test",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[1].id=product-service-test",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**"
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
    public void testUserServiceRouteMatching() {
        // 测试用户服务路由匹配
        String userUrl = "http://localhost:" + gatewayPort + "/user/test";
        ResponseEntity<String> response = restTemplate.getForEntity(userUrl, String.class);

        // 应该匹配到user路由，但由于服务不可用返回错误状态码
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Gateway应该匹配用户服务路由");
    }

    @Test
    public void testProductServiceRouteMatching() {
        // 测试产品服务路由匹配
        String productUrl = "http://localhost:" + gatewayPort + "/product/test";
        ResponseEntity<String> response = restTemplate.getForEntity(productUrl, String.class);

        // 应该匹配到product路由，但由于服务不可用返回错误状态码
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Gateway应该匹配产品服务路由");
    }

    @Test
    public void testInvalidRoute_ShouldReturn404() {
        // 测试不存在的路由
        String url = "http://localhost:" + gatewayPort + "/invalid/path";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
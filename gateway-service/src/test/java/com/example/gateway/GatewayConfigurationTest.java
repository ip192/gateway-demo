package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for validating Gateway configuration properties, route configuration,
 * and application startup as part of the gateway service simplification.
 * 
 * Requirements: 3.3 - Configuration validation and basic property loading
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=user-service",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[1].id=product-service",
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**",
    "spring.cloud.gateway.httpclient.connect-timeout=5000",
    "spring.cloud.gateway.httpclient.response-timeout=10s"
})
class GatewayConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private RouteLocator routeLocator;

    @LocalServerPort
    private int serverPort;

    @Value("${spring.cloud.gateway.httpclient.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${spring.cloud.gateway.httpclient.response-timeout:10s}")
    private String responseTimeout;

    @Value("${logging.level.org.springframework.cloud.gateway:INFO}")
    private String gatewayLogLevel;

    @Value("${logging.level.com.example.gateway:INFO}")
    private String applicationLogLevel;

    @Test
    void testApplicationStartup() {
        // 验证应用启动成功
        assertNotNull(applicationContext, "Application context should be loaded");
        if (applicationContext instanceof ConfigurableApplicationContext) {
            assertTrue(((ConfigurableApplicationContext) applicationContext).isActive(), 
                "Application context should be active");
        }
        
        // 验证核心组件存在
        assertNotNull(routeLocator, "RouteLocator should be available");
        assertTrue(serverPort > 0, "Server port should be configured");
    }

    @Test
    void testBasicConfigurationPropertiesLoading() {
        // 验证基本配置属性加载
        assertEquals(5000, connectTimeout, "HTTP client connect timeout should be 5000ms");
        assertEquals("10s", responseTimeout, "HTTP client response timeout should be 10s");
        assertEquals("INFO", gatewayLogLevel, "Gateway log level should be INFO");
        assertEquals("INFO", applicationLogLevel, "Application log level should be INFO");
    }

    @Test
    void testRouteConfigurationLoading() {
        // 验证路由配置加载
        Flux<Route> routes = routeLocator.getRoutes();
        
        StepVerifier.create(routes.collectList())
            .assertNext(routeList -> {
                assertNotNull(routeList, "Route list should not be null");
                assertEquals(2, routeList.size(), "Should have exactly 2 routes configured");
                
                // 验证路由ID和URI
                List<String> routeIds = routeList.stream()
                    .map(Route::getId)
                    .collect(Collectors.toList());
                
                assertTrue(routeIds.contains("user-service"), "Should contain user-service route");
                assertTrue(routeIds.contains("product-service"), "Should contain product-service route");
                
                // 验证路由URI
                routeList.forEach(route -> {
                    URI uri = route.getUri();
                    assertNotNull(uri, "Route URI should not be null for route: " + route.getId());
                    
                    if ("user-service".equals(route.getId())) {
                        assertEquals("http://localhost:8081", uri.toString(), 
                            "User service URI should be http://localhost:8081");
                    } else if ("product-service".equals(route.getId())) {
                        assertEquals("http://localhost:8082", uri.toString(), 
                            "Product service URI should be http://localhost:8082");
                    }
                });
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    void testRoutePredicateConfiguration() {
        // 验证路由谓词配置
        Flux<Route> routes = routeLocator.getRoutes();
        
        StepVerifier.create(routes.collectList())
            .assertNext(routeList -> {
                routeList.forEach(route -> {
                    assertNotNull(route.getPredicate(), 
                        "Route predicate should not be null for route: " + route.getId());
                    
                    // 验证路由谓词存在且配置正确
                    String routeId = route.getId();
                    assertTrue(routeId.equals("user-service") || routeId.equals("product-service"),
                        "Route ID should be either user-service or product-service");
                });
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    void testGatewayApplicationBeanConfiguration() {
        // 验证Gateway应用的Bean配置
        assertTrue(applicationContext.containsBean("gatewayApplication"), 
            "GatewayApplication bean should be present");
        
        // 验证RouteLocator Bean存在
        assertTrue(applicationContext.containsBean("routeLocator") || 
                  applicationContext.getBeansOfType(RouteLocator.class).size() > 0,
            "RouteLocator bean should be present");
    }

    @Test
    void testConfigurationConsistency() {
        // 验证配置一致性
        Flux<Route> routes = routeLocator.getRoutes();
        
        StepVerifier.create(routes.collectList())
            .assertNext(routeList -> {
                // 验证没有重复的路由ID
                List<String> routeIds = routeList.stream()
                    .map(Route::getId)
                    .collect(Collectors.toList());
                
                assertEquals(routeIds.size(), routeIds.stream().distinct().count(),
                    "Route IDs should be unique");
                
                // 验证所有路由都有有效的URI
                routeList.forEach(route -> {
                    URI uri = route.getUri();
                    assertNotNull(uri, "Route URI should not be null");
                    assertTrue(uri.toString().startsWith("http://"), 
                        "Route URI should start with http://");
                    assertTrue(uri.getPort() > 0, "Route URI should have a valid port");
                });
            })
            .expectComplete()
            .verify(Duration.ofSeconds(5));
    }

    @Test
    void testMinimalConfigurationRequirements() {
        // 验证最小配置要求
        assertNotNull(applicationContext, "Application context is required");
        assertNotNull(routeLocator, "RouteLocator is required for routing");
        
        // 验证基本的Spring Cloud Gateway配置存在
        assertTrue(connectTimeout > 0, "Connect timeout should be positive");
        assertNotNull(responseTimeout, "Response timeout should be configured");
        assertFalse(responseTimeout.isEmpty(), "Response timeout should not be empty");
        
        // 验证日志配置
        assertNotNull(gatewayLogLevel, "Gateway log level should be configured");
        assertNotNull(applicationLogLevel, "Application log level should be configured");
    }
}
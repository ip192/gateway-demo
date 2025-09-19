package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.routes[0].id=user-service",
    "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
    "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
    "spring.cloud.gateway.routes[1].id=product-service", 
    "spring.cloud.gateway.routes[1].uri=http://localhost:8082",
    "spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**"
})
class GatewayRoutingPropertiesTest {

    @Value("${spring.cloud.gateway.routes[0].id:}")
    private String userServiceRouteId;
    
    @Value("${spring.cloud.gateway.routes[0].uri:}")
    private String userServiceUri;
    
    @Value("${spring.cloud.gateway.routes[1].id:}")
    private String productServiceRouteId;
    
    @Value("${spring.cloud.gateway.routes[1].uri:}")
    private String productServiceUri;

    @Test
    void testUserServiceRouteProperties() {
        assertEquals("user-service", userServiceRouteId);
        assertEquals("http://localhost:8081", userServiceUri);
    }

    @Test
    void testProductServiceRouteProperties() {
        assertEquals("product-service", productServiceRouteId);
        assertEquals("http://localhost:8082", productServiceUri);
    }

    @Test
    void testRoutePropertiesNotEmpty() {
        assertNotNull(userServiceRouteId);
        assertNotNull(userServiceUri);
        assertNotNull(productServiceRouteId);
        assertNotNull(productServiceUri);
        
        assertFalse(userServiceRouteId.isEmpty());
        assertFalse(userServiceUri.isEmpty());
        assertFalse(productServiceRouteId.isEmpty());
        assertFalse(productServiceUri.isEmpty());
    }
}
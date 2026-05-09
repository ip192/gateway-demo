package com.example.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 路由配置测试类
 * 测试动态路由配置属性和基本配置
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {RoutingConfig.class, DynamicRoutingProperties.class})
@TestPropertySource(properties = {
    "gateway.dynamic-routing.enabled=true"
})
@EnableConfigurationProperties(DynamicRoutingProperties.class)
class RoutingConfigTest {
    
    @Autowired
    private DynamicRoutingProperties dynamicRoutingProperties;
    
    @Autowired
    private RoutingConfig routingConfig;
    
    @BeforeEach
    void setUp() {
        // Using autowired beans from Spring Boot test context
    }
    
    @Test
    void testRoutingConfigExists() {
        // 测试路由配置类的存在
        assertNotNull(routingConfig);
    }
    
    @Test
    void testDynamicRoutingPropertiesInjection() {
        // 测试动态路由属性的注入
        // Since we're using real Spring Boot test, the properties should be injected from test configuration
        assertNotNull(dynamicRoutingProperties);
        // The property should be enabled from our test configuration
        assertTrue(dynamicRoutingProperties.isEnabled());
    }
    
    @Test
    void testRoutingConfigConditionalProperty() {
        // 测试条件属性配置
        // 这个测试验证@ConditionalOnProperty注解的存在
        assertTrue(RoutingConfig.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class));
        
        org.springframework.boot.autoconfigure.condition.ConditionalOnProperty annotation = 
                RoutingConfig.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class);
        
        assertEquals("gateway.dynamic-routing.enabled", annotation.name()[0]);
        assertEquals("true", annotation.havingValue());
        assertTrue(annotation.matchIfMissing());
    }
}
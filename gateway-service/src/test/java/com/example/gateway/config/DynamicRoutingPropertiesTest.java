package com.example.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "gateway.dynamic-routing.enabled=false",
    "gateway.dynamic-routing.timeout.connect=3000",
    "gateway.dynamic-routing.timeout.read=8000",
    "gateway.dynamic-routing.retry.max-attempts=5",
    "gateway.dynamic-routing.retry.delay=2000"
})
class DynamicRoutingPropertiesTest {

    @Autowired
    private DynamicRoutingProperties properties;

    @Test
    void testPropertiesBinding() {
        assertNotNull(properties);
        assertFalse(properties.isEnabled());
        assertEquals(3000, properties.getTimeout().getConnect());
        assertEquals(8000, properties.getTimeout().getRead());
        assertEquals(5, properties.getRetry().getMaxAttempts());
        assertEquals(2000, properties.getRetry().getDelay());
    }

    @Test
    void testDefaultValues() {
        DynamicRoutingProperties defaultProps = new DynamicRoutingProperties();
        
        assertTrue(defaultProps.isEnabled());
        assertEquals(5000, defaultProps.getTimeout().getConnect());
        assertEquals(10000, defaultProps.getTimeout().getRead());
        assertEquals(3, defaultProps.getRetry().getMaxAttempts());
        assertEquals(1000, defaultProps.getRetry().getDelay());
    }

    @Test
    void testTimeoutConfiguration() {
        assertNotNull(properties.getTimeout());
        assertTrue(properties.getTimeout().getConnect() > 0);
        assertTrue(properties.getTimeout().getRead() > 0);
    }

    @Test
    void testRetryConfiguration() {
        assertNotNull(properties.getRetry());
        assertTrue(properties.getRetry().getMaxAttempts() > 0);
        assertTrue(properties.getRetry().getDelay() > 0);
    }
}
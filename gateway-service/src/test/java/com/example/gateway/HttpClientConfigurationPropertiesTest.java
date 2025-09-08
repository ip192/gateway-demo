package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.context.ApplicationContext;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HTTP client configuration validation from properties files
 * Verifies that HTTP client timeout and connection pool settings are correctly loaded
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.cloud.gateway.httpclient.connect-timeout=8000",
    "spring.cloud.gateway.httpclient.response-timeout=15s",
    "spring.cloud.gateway.httpclient.pool.max-connections=200",
    "spring.cloud.gateway.httpclient.pool.max-idle-time=45s",
    "spring.cloud.gateway.httpclient.pool.max-life-time=60s",
    "spring.cloud.gateway.httpclient.pool.acquire-timeout=10000"
})
class HttpClientConfigurationPropertiesTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private HttpClientProperties httpClientProperties;

    @Test
    void testHttpClientPropertiesLoaded() {
        assertThat(httpClientProperties).isNotNull();
    }

    @Test
    void testConnectTimeoutConfiguration() {
        assertThat(httpClientProperties.getConnectTimeout()).isEqualTo(8000);
    }

    @Test
    void testResponseTimeoutConfiguration() {
        assertThat(httpClientProperties.getResponseTimeout()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    void testConnectionPoolConfiguration() {
        HttpClientProperties.Pool pool = httpClientProperties.getPool();
        assertThat(pool).isNotNull();
        assertThat(pool.getMaxConnections()).isEqualTo(200);
        assertThat(pool.getMaxIdleTime()).isEqualTo(Duration.ofSeconds(45));
        assertThat(pool.getMaxLifeTime()).isEqualTo(Duration.ofSeconds(60));
        assertThat(pool.getAcquireTimeout()).isEqualTo(10000);
    }

    @Test
    void testHttpClientBeanExists() {
        // Verify that HttpClient bean is created and configured
        assertThat(applicationContext.containsBean("gatewayHttpClient")).isTrue();
    }

    @Test
    void testHttpClientConfiguration() {
        // Test that we can get the HttpClient bean
        HttpClient httpClient = applicationContext.getBean("gatewayHttpClient", HttpClient.class);
        assertThat(httpClient).isNotNull();
    }

    @Test
    void testDefaultHttpClientProperties() {
        // Test with default properties to ensure they are reasonable
        HttpClientProperties defaultProperties = new HttpClientProperties();
        
        // Verify default values exist and are reasonable
        assertThat(defaultProperties.getConnectTimeout()).isNotNull();
        assertThat(defaultProperties.getConnectTimeout()).isGreaterThan(0);
        
        if (defaultProperties.getResponseTimeout() != null) {
            assertThat(defaultProperties.getResponseTimeout().toMillis()).isGreaterThan(0);
        }
    }

    @Test
    void testConnectionPoolDefaults() {
        HttpClientProperties defaultProperties = new HttpClientProperties();
        HttpClientProperties.Pool pool = defaultProperties.getPool();
        
        assertThat(pool).isNotNull();
        // Verify that pool has reasonable defaults
        if (pool.getMaxConnections() != null) {
            assertThat(pool.getMaxConnections()).isGreaterThan(0);
        }
    }

    @Test
    void testTimeoutValidation() {
        // Verify that timeout values are properly parsed and validated
        assertThat(httpClientProperties.getConnectTimeout()).isGreaterThan(0);
        assertThat(httpClientProperties.getResponseTimeout().toMillis()).isGreaterThan(0);
        
        // Verify timeout values are reasonable (not too small or too large)
        assertThat(httpClientProperties.getConnectTimeout()).isLessThan(60000); // Less than 60 seconds
        assertThat(httpClientProperties.getResponseTimeout().toMillis()).isLessThan(300000L); // Less than 5 minutes
    }

    @Test
    void testConnectionPoolValidation() {
        HttpClientProperties.Pool pool = httpClientProperties.getPool();
        
        // Verify pool configuration values are reasonable
        assertThat(pool.getMaxConnections()).isGreaterThan(0);
        assertThat(pool.getMaxConnections()).isLessThan(10000); // Reasonable upper limit
        
        assertThat(pool.getMaxIdleTime().toMillis()).isGreaterThan(0);
        assertThat(pool.getMaxIdleTime().toMillis()).isLessThan(3600000L); // Less than 1 hour
        
        assertThat(pool.getAcquireTimeout()).isGreaterThan(0);
        assertThat(pool.getAcquireTimeout()).isLessThan(60000); // Less than 60 seconds
    }
}
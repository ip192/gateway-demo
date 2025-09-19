package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BasicErrorHandlingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testServiceUnavailable_UserService_ShouldReturnError() {
        // Test when user service is not running
        String url = "http://localhost:" + port + "/user/test";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Spring Cloud Gateway returns 500 when downstream service is unavailable
        assertTrue(response.getStatusCode().is5xxServerError(),
                  "Expected 5xx error but got: " + response.getStatusCode());
    }

    @Test
    public void testServiceUnavailable_ProductService_ShouldReturnError() {
        // Test when product service is not running
        String url = "http://localhost:" + port + "/product/test";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Spring Cloud Gateway returns 500 when downstream service is unavailable
        assertTrue(response.getStatusCode().is5xxServerError(),
                  "Expected 5xx error but got: " + response.getStatusCode());
    }

    @Test
    public void testInvalidPath_ShouldReturnNotFound() {
        // Test invalid path that doesn't match any route
        String url = "http://localhost:" + port + "/invalid/path";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testInvalidPath_RootPath_ShouldReturnNotFound() {
        // Test root path which should not be routed
        String url = "http://localhost:" + port + "/";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testInvalidPath_UnmatchedService_ShouldReturnNotFound() {
        // Test path that looks like a service but doesn't match configured routes
        String url = "http://localhost:" + port + "/order/123";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    public void testNetworkTimeout_UserService_ShouldHandleTimeout() {
        // This test verifies that the gateway handles connection failures gracefully
        String url = "http://localhost:" + port + "/user/slow-endpoint";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Spring Cloud Gateway returns 500 for connection failures
        assertTrue(response.getStatusCode().is5xxServerError(),
                  "Expected 5xx error but got: " + response.getStatusCode());
    }

    @Test
    public void testNetworkTimeout_ProductService_ShouldHandleTimeout() {
        // This test verifies that the gateway handles connection failures gracefully
        String url = "http://localhost:" + port + "/product/slow-endpoint";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Spring Cloud Gateway returns 500 for connection failures
        assertTrue(response.getStatusCode().is5xxServerError(),
                  "Expected 5xx error but got: " + response.getStatusCode());
    }

    @Test
    public void testMalformedRequest_ShouldHandleGracefully() {
        // Test that the gateway handles malformed requests gracefully
        String url = "http://localhost:" + port + "/user/../admin";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should either normalize the path or return an error
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Expected error status but got: " + response.getStatusCode());
    }

    @Test
    public void testLongPath_ShouldHandleGracefully() {
        // Test very long path to ensure gateway handles it gracefully
        StringBuilder longPath = new StringBuilder("/user/");
        for (int i = 0; i < 100; i++) {
            longPath.append("very-long-path-segment-").append(i).append("/");
        }
        
        String url = "http://localhost:" + port + longPath.toString();
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        // Should handle long paths gracefully (either route or return error)
        assertNotNull(response.getStatusCode());
        assertTrue(response.getStatusCode().is4xxClientError() || 
                  response.getStatusCode().is5xxServerError(),
                  "Expected error status for long path but got: " + response.getStatusCode());
    }
}
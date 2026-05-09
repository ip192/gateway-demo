package com.example.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpProxyHelperTest {

    @Mock
    private RestTemplate restTemplate;

    private HttpProxyHelper httpProxyHelper;

    @BeforeEach
    void setUp() {
        httpProxyHelper = new HttpProxyHelper(restTemplate);
    }

    @Test
    void testForwardRequest_Success() {
        // Arrange
        String targetUrl = "http://user-service:8081/user/test";
        HttpMethod method = HttpMethod.GET;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        Object body = "{\"test\": \"data\"}";
        
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok("success");
        when(restTemplate.exchange(eq(targetUrl), eq(method), any(HttpEntity.class), eq(Object.class)))
                .thenReturn(expectedResponse);

        // Act
        ResponseEntity<Object> result = httpProxyHelper.forwardRequest(targetUrl, method, headers, body);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("success", result.getBody());
        verify(restTemplate).exchange(eq(targetUrl), eq(method), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testFilterHeaders_RemovesFilteredHeaders() {
        // Arrange
        HttpHeaders originalHeaders = new HttpHeaders();
        originalHeaders.set("Content-Type", "application/json");
        originalHeaders.set("Authorization", "Bearer token123");
        originalHeaders.set("Host", "localhost:8080"); // 应该被过滤
        originalHeaders.set("Connection", "keep-alive"); // 应该被过滤
        originalHeaders.set("Content-Length", "100"); // 应该被过滤

        // Act
        HttpHeaders result = httpProxyHelper.filterHeaders(originalHeaders);

        // Assert
        assertTrue(result.containsKey("Content-Type"));
        assertTrue(result.containsKey("Authorization"));
        assertFalse(result.containsKey("Host"));
        assertFalse(result.containsKey("Connection"));
        assertFalse(result.containsKey("Content-Length"));
        assertEquals("application/json", result.getFirst("Content-Type"));
    }

    @Test
    void testFilterHeaders_EmptyHeaders() {
        // Arrange
        HttpHeaders originalHeaders = new HttpHeaders();

        // Act
        HttpHeaders result = httpProxyHelper.filterHeaders(originalHeaders);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void testFilterHeaders_MultipleValuesForSameHeader() {
        // Arrange
        HttpHeaders originalHeaders = new HttpHeaders();
        originalHeaders.add("Accept", "application/json");
        originalHeaders.add("Accept", "application/xml");

        // Act
        HttpHeaders result = httpProxyHelper.filterHeaders(originalHeaders);

        // Assert
        assertTrue(result.containsKey("Accept"));
        assertEquals(2, result.get("Accept").size());
        assertTrue(result.get("Accept").contains("application/json"));
        assertTrue(result.get("Accept").contains("application/xml"));
    }

    @Test
    void testBuildQueryString_WithParameters() {
        // Arrange
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("name", "John");
        queryParams.add("age", "25");
        queryParams.add("tags", "java");
        queryParams.add("tags", "spring");

        // Act
        String result = httpProxyHelper.buildQueryString(queryParams);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=25"));
        assertTrue(result.contains("tags=java"));
        assertTrue(result.contains("tags=spring"));
    }

    @Test
    void testBuildQueryString_EmptyParameters() {
        // Arrange
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

        // Act
        String result = httpProxyHelper.buildQueryString(queryParams);

        // Assert
        assertEquals("", result);
    }

    @Test
    void testBuildQueryString_NullParameters() {
        // Act
        String result = httpProxyHelper.buildQueryString(null);

        // Assert
        assertEquals("", result);
    }

    @Test
    void testBuildQueryString_ParameterWithoutValue() {
        // Arrange
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("flag", "");
        queryParams.add("debug", null);

        // Act
        String result = httpProxyHelper.buildQueryString(queryParams);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("flag") || result.contains("debug"));
    }

    @Test
    void testBuildQueryString_MultipleValuesForSameParameter() {
        // Arrange
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("category", "tech");
        queryParams.add("category", "news");
        queryParams.add("category", "sports");

        // Act
        String result = httpProxyHelper.buildQueryString(queryParams);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("category=tech"));
        assertTrue(result.contains("category=news"));
        assertTrue(result.contains("category=sports"));
    }
}
package com.example.gateway;

import com.example.gateway.filter.ResponseFormattingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResponseFormattingFilterTest {

    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpResponse response;
    
    @Mock
    private GatewayFilterChain chain;
    
    private HttpHeaders requestHeaders;
    private HttpHeaders responseHeaders;
    private ResponseFormattingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ResponseFormattingFilter();
        requestHeaders = new HttpHeaders();
        responseHeaders = new HttpHeaders();
        
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(request.getHeaders()).thenReturn(requestHeaders);
        lenient().when(response.getHeaders()).thenReturn(responseHeaders);
        lenient().when(request.getURI()).thenReturn(URI.create("http://localhost:8080/test"));
        lenient().when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    }

    @Test
    void shouldPassThroughSuccessfulRequests() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain).filter(exchange);
    }

    @Test
    void shouldAddStandardHeaders() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assert responseHeaders.containsKey("X-Content-Type-Options");
        assert responseHeaders.containsKey("X-Frame-Options");
        assert responseHeaders.containsKey("X-XSS-Protection");
        assert responseHeaders.containsKey("X-Gateway-Version");
        assert responseHeaders.containsKey("X-Response-Time");
        
        assertEquals("nosniff", responseHeaders.getFirst("X-Content-Type-Options"));
        assertEquals("DENY", responseHeaders.getFirst("X-Frame-Options"));
        assertEquals("1; mode=block", responseHeaders.getFirst("X-XSS-Protection"));
        assertEquals("1.0", responseHeaders.getFirst("X-Gateway-Version"));
    }

    @Test
    void shouldAddCacheControlHeaders() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assert responseHeaders.containsKey(HttpHeaders.CACHE_CONTROL);
        assertEquals("no-cache, no-store, must-revalidate", 
                responseHeaders.getFirst(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void shouldNotOverrideCacheControlIfPresent() {
        // Given
        responseHeaders.setCacheControl("public, max-age=3600");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals("public, max-age=3600", 
                responseHeaders.getFirst(HttpHeaders.CACHE_CONTROL));
    }

    @Test
    void shouldAddCorsHeadersWhenOriginPresent() {
        // Given
        requestHeaders.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals("http://localhost:3000", 
                responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals("true", 
                responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals("GET, POST, PUT, DELETE, OPTIONS", 
                responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS));
        assertEquals("Content-Type, Authorization, X-Requested-With", 
                responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
        assertEquals("3600", 
                responseHeaders.getFirst(HttpHeaders.ACCESS_CONTROL_MAX_AGE));
    }

    @Test
    void shouldNotAddCorsHeadersWhenNoOrigin() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assert !responseHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        assert !responseHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
    }

    @Test
    void shouldSetJsonContentTypeForApiResponses() {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals(MediaType.APPLICATION_JSON, 
                responseHeaders.getContentType());
    }

    @Test
    void shouldNotOverrideExistingContentType() {
        // Given
        responseHeaders.setContentType(MediaType.TEXT_PLAIN);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals(MediaType.TEXT_PLAIN, 
                responseHeaders.getContentType());
    }

    @Test
    void shouldHandleErrorResponses() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        // Standard headers should still be added for error responses
        assert responseHeaders.containsKey("X-Content-Type-Options");
        assert responseHeaders.containsKey("X-Frame-Options");
    }

    @Test
    void shouldHaveCorrectOrder() {
        // When
        int order = filter.getOrder();

        // Then
        assertEquals(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 10, order);
    }

    @Test
    void shouldSetJsonContentTypeFor4xxErrors() {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals(MediaType.APPLICATION_JSON, 
                responseHeaders.getContentType());
    }

    @Test
    void shouldSetJsonContentTypeFor5xxErrors() {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        assertEquals(MediaType.APPLICATION_JSON, 
                responseHeaders.getContentType());
    }
}
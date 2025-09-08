package com.example.gateway;

import com.example.gateway.filter.RequestLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpResponse response;
    
    @Mock
    private GatewayFilterChain chain;

    private RequestLoggingFilter filter;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        filter = new RequestLoggingFilter();
        attributes = new HashMap<>();
        
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(exchange.getAttributes()).thenReturn(attributes);
        lenient().when(exchange.getAttribute("startTime")).thenAnswer(invocation -> attributes.get("startTime"));
        
        // Setup default request properties
        lenient().when(request.getMethod()).thenReturn(HttpMethod.GET);
        lenient().when(request.getURI()).thenReturn(URI.create("http://localhost:8080/test"));
        lenient().when(request.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
        lenient().when(request.getHeaders()).thenReturn(org.springframework.http.HttpHeaders.EMPTY);
        
        // Setup default response properties
        lenient().when(response.getStatusCode()).thenReturn(HttpStatus.OK);
    }

    @Test
    void shouldLogRequestAndResponse() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(chain).filter(exchange);
        // Verify that start time was recorded
        assert attributes.containsKey("startTime");
        assert attributes.get("startTime") instanceof Long;
    }

    @Test
    void shouldRecordStartTime() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        filter.filter(exchange, chain).block();

        // Then
        Long startTime = (Long) attributes.get("startTime");
        assert startTime != null;
        assert startTime > 0;
        assert startTime <= System.currentTimeMillis();
    }

    @Test
    void shouldHandleErrorsGracefully() {
        // Given
        RuntimeException testException = new RuntimeException("Test error");
        when(chain.filter(exchange)).thenReturn(Mono.error(testException));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        // Verify that start time was still recorded
        assert attributes.containsKey("startTime");
    }

    @Test
    void shouldHaveCorrectOrder() {
        // When
        int order = filter.getOrder();

        // Then
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1, order);
    }

    @Test
    void shouldLogRequestDetails() {
        // Given
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/users"));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        verify(request, atLeastOnce()).getMethod();
        verify(request, atLeastOnce()).getURI();
        verify(request, atLeastOnce()).getRemoteAddress();
    }

    @Test
    void shouldLogResponseDetails() {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.CREATED);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        verify(response).getStatusCode();
    }

    @Test
    void shouldCalculateProcessingTime() {
        // Given
        when(chain.filter(exchange)).thenReturn(Mono.empty());
        
        // When
        long beforeFilter = System.currentTimeMillis();
        filter.filter(exchange, chain).block();
        long afterFilter = System.currentTimeMillis();

        // Then
        Long startTime = (Long) attributes.get("startTime");
        assert startTime != null;
        assert startTime >= beforeFilter;
        assert startTime <= afterFilter;
    }
}
package com.example.gateway;

import com.example.gateway.filter.ErrorHandlingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErrorHandlingFilterTest {

    @Mock
    private ServerWebExchange exchange;
    
    @Mock
    private ServerHttpRequest request;
    
    @Mock
    private ServerHttpResponse response;
    
    @Mock
    private GatewayFilterChain chain;
    
    @Mock
    private HttpHeaders headers;

    private ErrorHandlingFilter filter;
    private DataBufferFactory dataBufferFactory;

    @BeforeEach
    void setUp() {
        filter = new ErrorHandlingFilter();
        dataBufferFactory = new DefaultDataBufferFactory();
        
        lenient().when(exchange.getRequest()).thenReturn(request);
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(response.getHeaders()).thenReturn(headers);
        lenient().when(response.bufferFactory()).thenReturn(dataBufferFactory);
        lenient().when(request.getURI()).thenReturn(URI.create("http://localhost:8080/test"));
        
        // Mock response.writeWith to return completed Mono
        lenient().when(response.writeWith(any())).thenReturn(Mono.empty());
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
        verifyNoInteractions(response);
    }

    @Test
    void shouldHandleResponseStatusException() {
        // Given
        ResponseStatusException exception = new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Invalid request");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void shouldHandleConnectException() {
        // Given
        ConnectException exception = new ConnectException("Connection refused");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void shouldHandleTimeoutException() {
        // Given
        TimeoutException exception = new TimeoutException("Request timeout");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.GATEWAY_TIMEOUT);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void shouldHandle404Errors() {
        // Given
        RuntimeException exception = new RuntimeException("404 Not Found");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void shouldHandleGenericExceptions() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(any());
    }

    @Test
    void shouldHaveCorrectOrder() {
        // When
        int order = filter.getOrder();

        // Then
        assertEquals(org.springframework.core.Ordered.LOWEST_PRECEDENCE - 1, order);
    }

    @Test
    void shouldHandleSerializationErrors() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));
        when(response.writeWith(any())).thenThrow(new RuntimeException("Serialization error"));
        when(response.setComplete()).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(response).setComplete();
    }

    @Test
    void shouldCreateStandardizedErrorResponse() {
        // Given
        RuntimeException exception = new RuntimeException("Test error");
        when(chain.filter(exchange)).thenReturn(Mono.error(exception));

        // When
        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        // Then
        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(headers).setContentType(MediaType.APPLICATION_JSON);
        verify(response).writeWith(argThat(mono -> {
            // Verify that a DataBuffer is written
            return mono != null;
        }));
    }
}
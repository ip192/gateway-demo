package com.example.gateway;

import com.example.gateway.controller.FallbackController;
import com.example.gateway.model.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FallbackController
 */
public class FallbackControllerTest {

    private FallbackController fallbackController;
    private ServerWebExchange mockExchange;

    @BeforeEach
    void setUp() {
        fallbackController = new FallbackController();
        mockExchange = mock(ServerWebExchange.class);
        
        // Mock the request path
        org.springframework.http.server.reactive.ServerHttpRequest mockRequest = 
            mock(org.springframework.http.server.reactive.ServerHttpRequest.class);
        org.springframework.http.server.RequestPath mockPath = 
            mock(org.springframework.http.server.RequestPath.class);
        
        when(mockExchange.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getPath()).thenReturn(mockPath);
        when(mockPath.toString()).thenReturn("/test/path");
    }

    @Test
    void testUserServiceFallback() {
        Mono<ResponseEntity<ApiResponse<Object>>> result = 
            fallbackController.userServiceFallback(mockExchange);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getMessage())
                    .contains("User service is temporarily unavailable");
                assertThat(response.getBody().getData()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void testProductServiceFallback() {
        Mono<ResponseEntity<ApiResponse<Object>>> result = 
            fallbackController.productServiceFallback(mockExchange);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getMessage())
                    .contains("Product service is temporarily unavailable");
                assertThat(response.getBody().getData()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void testGeneralFallback() {
        Mono<ResponseEntity<ApiResponse<Object>>> result = 
            fallbackController.generalFallback(mockExchange);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getMessage())
                    .contains("Service is temporarily unavailable");
                assertThat(response.getBody().getData()).isNull();
            })
            .verifyComplete();
    }

    @Test
    void testTimeoutFallback() {
        Mono<ResponseEntity<ApiResponse<Object>>> result = 
            fallbackController.timeoutFallback(mockExchange);

        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.REQUEST_TIMEOUT);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isFalse();
                assertThat(response.getBody().getMessage())
                    .contains("Request timeout");
                assertThat(response.getBody().getData()).isNull();
            })
            .verifyComplete();
    }
}
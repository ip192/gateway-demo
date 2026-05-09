package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Arrays;

@Configuration
public class LoadBalancerConfig {
    
    @Autowired
    private DynamicRoutingProperties dynamicRoutingProperties;
    
    @Bean
    @Primary
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    @Bean("dynamicRoutingRestTemplate")
    public RestTemplate dynamicRoutingRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure timeouts using HttpComponentsClientHttpRequestFactory
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setConnectTimeout(dynamicRoutingProperties.getTimeout().getConnect());
        factory.setReadTimeout(dynamicRoutingProperties.getTimeout().getRead());
        restTemplate.setRequestFactory(factory);
        
        // Add retry interceptor
        restTemplate.setInterceptors(Arrays.asList(new RetryInterceptor()));
        
        return restTemplate;
    }
    
    /**
     * Retry interceptor for handling temporary network issues.
     */
    private class RetryInterceptor implements ClientHttpRequestInterceptor {
        
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                          ClientHttpRequestExecution execution) throws IOException {
            int maxAttempts = dynamicRoutingProperties.getRetry().getMaxAttempts();
            long delay = dynamicRoutingProperties.getRetry().getDelay();
            
            IOException lastException = null;
            
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    return execution.execute(request, body);
                } catch (IOException e) {
                    lastException = e;
                    
                    // Don't retry on the last attempt
                    if (attempt == maxAttempts) {
                        break;
                    }
                    
                    // Wait before retrying
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Retry interrupted", ie);
                    }
                }
            }
            
            throw lastException;
        }
    }
}
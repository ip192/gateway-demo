package com.example.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductServiceClient {
    
    @GetMapping("/product/{id}")
    ResponseEntity<Object> getProduct(@PathVariable("id") String id);
    
    @PostMapping("/product/info")
    ResponseEntity<Object> getProductInfo(@RequestBody Object request);
}
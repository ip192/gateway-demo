package com.example.gateway.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @GetMapping("/user/{id}")
    ResponseEntity<Object> getUser(@PathVariable("id") String id);
    
    @PostMapping("/user/info")
    ResponseEntity<Object> getUserInfo(@RequestBody Object request);
}
package com.example.product;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {

    @PostMapping("/add")
    public ApiResponse add() {
        return new ApiResponse("产品添加成功", "success");
    }

    @PostMapping("/query")
    public ApiResponse query() {
        return new ApiResponse("产品查询成功", "success");
    }

    @PostMapping("/info")
    public ApiResponse info(@RequestBody InfoRequest request) {
        return new ApiResponse("product info for id: " + request.getId(), "success");
    }

    // Handle specific 404 case
    @PostMapping("/nonexistent")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "请求的资源不存在");
        response.put("status", "error");
        response.put("timestamp", LocalDateTime.now());
        return response;
    }
}
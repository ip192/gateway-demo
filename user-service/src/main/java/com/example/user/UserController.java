package com.example.user;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @PostMapping("/login")
    public ApiResponse login() {
        return new ApiResponse("登录成功", "success");
    }

    @PostMapping("/logout")
    public ApiResponse logout() {
        return new ApiResponse("登出成功", "success");
    }

    @PostMapping("/info")
    public ApiResponse info(@RequestBody InfoRequest request) {
        return new ApiResponse("user info for id: " + request.getId(), "success");
    }
}
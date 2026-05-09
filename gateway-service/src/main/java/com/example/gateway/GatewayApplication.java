package com.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = {
    org.springframework.cloud.gateway.config.GatewayAutoConfiguration.class,
    org.springframework.cloud.gateway.config.GatewayClassPathWarningAutoConfiguration.class
})
@EnableEurekaClient
@EnableFeignClients
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
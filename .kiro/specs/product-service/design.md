# 设计文档

## 概述

产品服务是一个Spring Boot微服务，遵循与现有用户服务相同的架构模式。该服务将提供产品管理的REST API端点，并通过Spring Cloud Gateway进行路由。Gateway服务将使用Feign客户端来调用产品服务和用户服务，实现服务间的通信。

## 架构

### 整体架构
```
客户端 -> Gateway服务 (端口8080) -> Feign客户端 -> 产品服务 (端口8082)
                                              -> 用户服务 (端口8081)
```

### 服务端口分配
- Gateway服务: 8080
- 用户服务: 8081  
- 产品服务: 8082 (新增)

### 技术栈
- Spring Boot 2.7.18
- Spring Cloud 2021.0.8
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Maven多模块项目

## 组件和接口

### 产品服务组件

#### 1. ProductController
- 路径: `/product`
- 端点:
  - `POST /product/add` - 添加产品
  - `POST /product/query` - 查询产品

#### 2. ApiResponse (共享)
- 与用户服务使用相同的响应格式
- 字段: `message`, `status`

#### 3. 异常处理
- 使用与用户服务相同的GlobalExceptionHandler模式

### Gateway服务增强

#### 1. Feign客户端接口
```java
@FeignClient(name = "user-service", url = "http://localhost:8081")
public interface UserServiceClient {
    @PostMapping("/user/login")
    ApiResponse login();
    
    @PostMapping("/user/logout") 
    ApiResponse logout();
}

@FeignClient(name = "product-service", url = "http://localhost:8082")
public interface ProductServiceClient {
    @PostMapping("/product/add")
    ApiResponse addProduct();
    
    @PostMapping("/product/query")
    ApiResponse queryProduct();
}
```

#### 2. Gateway控制器
- 新增控制器来处理通过Feign的服务调用
- 提供统一的API入口点

#### 3. 路由配置
- 更新application.properties以包含产品服务路由
- 配置负载均衡和错误处理

## 数据模型

### ApiResponse
```java
public class ApiResponse {
    private String message;
    private String status;
    // 构造函数、getter、setter
}
```

### 产品相关响应消息
- 添加产品成功: "产品添加成功"
- 查询产品成功: "产品查询成功"
- 状态: "success"

## 错误处理

### 产品服务错误处理
- 实现GlobalExceptionHandler
- 返回统一的错误响应格式
- 处理常见异常类型

### Feign客户端错误处理
- 配置Feign错误解码器
- 实现服务降级机制
- 超时和重试配置

### Gateway错误处理
- 处理下游服务不可用的情况
- 提供友好的错误响应
- 日志记录和监控

## 测试策略

### 单元测试
- ProductController测试
- Feign客户端测试
- 异常处理测试

### 集成测试
- Gateway到产品服务的端到端测试
- Feign客户端集成测试
- 多服务协作测试

### 测试工具
- JUnit 5
- Spring Boot Test
- MockMvc
- WireMock (用于Feign客户端测试)

## 配置管理

### 产品服务配置 (application.properties)
```properties
server.port=8082
spring.application.name=product-service
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### Gateway服务配置更新
```properties
# 现有用户服务路由
spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**

# 新增产品服务路由
spring.cloud.gateway.routes[1].id=product-service
spring.cloud.gateway.routes[1].uri=http://localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/product/**

# Feign配置
feign.client.config.default.connect-timeout=5000
feign.client.config.default.read-timeout=5000
```

### Maven依赖更新

#### 根pom.xml更新
- 添加product-service模块

#### Gateway服务pom.xml更新
- 添加spring-cloud-starter-openfeign依赖

#### 产品服务pom.xml
- 复制用户服务的依赖结构
- 使用相同的Spring Boot和Spring Cloud版本

## 部署和运行

### 启动顺序
1. 产品服务 (端口8082)
2. 用户服务 (端口8081)
3. Gateway服务 (端口8080)

### 脚本更新
- 更新start-services.sh包含产品服务
- 更新stop-services.sh包含产品服务
- 更新test-services.sh包含产品服务测试

## 监控和健康检查

### Actuator端点
- 产品服务: http://localhost:8082/actuator/health
- Gateway服务健康检查包含下游服务状态

### 日志配置
- 统一的日志格式
- Feign客户端调用日志
- 错误和异常日志
# 设计文档

## 概述

本系统采用Spring Cloud微服务架构，包含两个核心服务：Gateway网关服务和User用户服务。Gateway服务基于Spring Cloud Gateway实现，作为系统的统一入口，负责路由转发和请求代理。User服务提供用户相关的业务接口，通过简单的REST API响应登录和登出请求。

## 架构

### 系统架构图

```mermaid
graph TB
    Client[客户端] --> Gateway[Gateway服务 端口8080]
    Gateway --> User[User服务 端口8081]
    
    subgraph GatewayService[Gateway服务]
        GW_Config[路由配置]
        GW_Filter[过滤器链]
    end
    
    subgraph UserService[User服务]
        Login[/user/login]
        Logout[/user/logout]
    end
```

### 服务通信流程

1. 客户端发送请求到Gateway服务 (http://localhost:8080)
2. Gateway根据路由配置将请求转发到User服务 (http://localhost:8081)
3. User服务处理请求并返回响应
4. Gateway将响应返回给客户端

## 组件和接口

### Gateway服务组件

#### 1. 主应用类
- `GatewayApplication.java`: Spring Boot启动类
- 启用Gateway功能和服务发现

#### 2. 路由配置
- `application.properties`: 包含路由规则配置
- 定义路径匹配规则和目标服务地址

#### 3. 依赖配置
- `pom.xml`: Maven依赖管理，基于Java 8
- 主要依赖：
  - spring-cloud-starter-gateway
  - spring-boot-starter-webflux

### User服务组件

#### 1. 主应用类
- `UserApplication.java`: Spring Boot启动类

#### 2. 控制器
- `UserController.java`: REST API控制器
- 提供登录和登出接口

#### 3. 依赖配置
- `pom.xml`: Maven依赖管理，基于Java 8
- 主要依赖：
  - spring-boot-starter-web

### 接口设计

#### 登录接口
- **路径**: POST /user/login
- **请求体**: JSON格式 (可选，用于演示)
- **响应**: 
  ```json
  {
    "message": "登录成功",
    "status": "success"
  }
  ```

#### 登出接口
- **路径**: POST /user/logout
- **请求体**: 无
- **响应**:
  ```json
  {
    "message": "登出成功", 
    "status": "success"
  }
  ```

## 数据模型

### 响应模型
```java
public class ApiResponse {
    private String message;
    private String status;
    
    // 构造函数、getter、setter
}
```

由于接口只返回简单消息，不需要复杂的数据模型。

## 错误处理

### Gateway层错误处理
- 服务不可用时返回503状态码
- 路由不匹配时返回404状态码
- 网络超时时返回504状态码

### User服务错误处理
- 全局异常处理器处理未预期的异常
- 返回标准化的错误响应格式

### 错误响应格式
```json
{
  "message": "错误描述",
  "status": "error",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

## 测试策略

### 单元测试
- Gateway服务：测试路由配置和过滤器逻辑
- User服务：测试控制器接口响应

### 集成测试
- 端到端测试：通过Gateway访问User服务接口
- 服务间通信测试：验证请求转发和响应返回

### 测试工具
- JUnit 5: 单元测试框架
- Spring Boot Test: 集成测试支持
- MockMvc: Web层测试
- Postman/curl: 手动接口测试

### 测试场景
1. 正常请求流程测试
2. 服务不可用场景测试
3. 错误路径测试
4. 并发请求测试

## 配置说明

### Gateway服务配置 (application.properties)
```properties
server.port=8080
spring.application.name=gateway-service
spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=http://localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**
```

### User服务配置 (application.properties)
```properties
server.port=8081
spring.application.name=user-service
```

## 部署考虑

### 端口分配
- Gateway服务: 8080
- User服务: 8081

### 启动顺序
1. 先启动User服务
2. 再启动Gateway服务

### 健康检查
- 两个服务都提供Spring Boot Actuator健康检查端点
- Gateway可以检查下游服务的健康状态
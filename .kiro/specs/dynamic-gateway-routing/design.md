# Design Document

## Overview

本设计将重构现有的gateway-service，从基于Feign客户端的硬编码转发模式转换为基于Spring Cloud Gateway的纯配置驱动路由模式。新架构将移除所有硬编码的controller方法和Feign客户端，改为使用Spring Cloud Gateway的声明式路由配置，实现真正的动态、可扩展的API网关。

## Architecture

### Current Architecture Issues
- **混合架构问题**: 同时使用Spring Cloud Gateway路由和Feign客户端转发，造成架构混乱
- **硬编码转发**: GatewayController中为每个下游接口都写了转发方法
- **可维护性差**: 新增下游接口需要修改Java代码
- **性能开销**: 不必要的Feign客户端调用增加了延迟

### Target Architecture
```
Client Request → Spring Cloud Gateway → Route Predicates → Filters → Downstream Services
```

### Key Components

1. **Route Configuration Manager**
   - 负责加载和管理路由配置
   - 支持配置热加载
   - 提供路由规则验证

2. **Dynamic Route Locator**
   - 基于配置动态创建路由
   - 支持多种路径匹配模式
   - 管理路由优先级

3. **Custom Gateway Filters**
   - 请求/响应日志记录
   - 错误处理和统一响应格式
   - 性能监控和指标收集

4. **Circuit Breaker Integration**
   - 替换Hystrix为Spring Cloud Circuit Breaker
   - 配置化的熔断策略
   - 自定义fallback处理

## Components and Interfaces

### 1. Route Configuration Structure

```yaml
gateway:
  routes:
    - id: user-service-routes
      uri: http://localhost:8081
      predicates:
        - Path=/user/**
      filters:
        - name: CircuitBreaker
          args:
            name: user-service-cb
            fallbackUri: forward:/fallback/user
        - name: Retry
          args:
            retries: 3
            backoff:
              firstBackoff: 50ms
              maxBackoff: 500ms
      metadata:
        timeout: 5000
        enabled: true
        
    - id: product-service-routes  
      uri: http://localhost:8082
      predicates:
        - Path=/product/**
      filters:
        - name: CircuitBreaker
          args:
            name: product-service-cb
            fallbackUri: forward:/fallback/product
      metadata:
        timeout: 5000
        enabled: true
```

### 2. Configuration Properties Class

```java
@ConfigurationProperties(prefix = "gateway")
public class GatewayRoutingProperties {
    private List<RouteDefinition> routes;
    private CircuitBreakerConfig circuitBreaker;
    private RetryConfig retry;
    // getters and setters
}
```

### 3. Dynamic Route Locator

```java
@Component
public class DynamicRouteLocator implements RouteLocator {
    private final GatewayRoutingProperties properties;
    
    @Override
    public Flux<Route> getRoutes() {
        // 基于配置动态创建路由
    }
}
```

### 4. Custom Gateway Filters

```java
@Component
public class RequestLoggingFilter implements GlobalFilter {
    // 请求日志记录
}

@Component  
public class ErrorHandlingFilter implements GlobalFilter {
    // 统一错误处理
}
```

### 5. Fallback Controller

```java
@RestController
public class FallbackController {
    
    @RequestMapping("/fallback/user")
    public ApiResponse userServiceFallback() {
        // 用户服务fallback逻辑
    }
    
    @RequestMapping("/fallback/product")
    public ApiResponse productServiceFallback() {
        // 产品服务fallback逻辑
    }
}
```

## Data Models

### Route Configuration Model

```java
public class RouteConfig {
    private String id;
    private String uri;
    private List<PredicateConfig> predicates;
    private List<FilterConfig> filters;
    private RouteMetadata metadata;
}

public class PredicateConfig {
    private String name;
    private Map<String, String> args;
}

public class FilterConfig {
    private String name;
    private Map<String, Object> args;
}

public class RouteMetadata {
    private int timeout;
    private boolean enabled;
    private int order;
}
```

### Circuit Breaker Configuration

```java
public class CircuitBreakerConfig {
    private int failureRateThreshold = 50;
    private int waitDurationInOpenState = 10000;
    private int slidingWindowSize = 10;
    private int minimumNumberOfCalls = 5;
}
```

## Error Handling

### 1. Global Exception Handler
- 统一处理所有网关级别的异常
- 返回标准化的错误响应格式
- 记录错误日志和监控指标

### 2. Circuit Breaker Fallback
- 为每个服务配置专门的fallback端点
- 返回有意义的降级响应
- 避免级联失败

### 3. Timeout Handling
- 配置化的请求超时时间
- 超时后自动触发fallback
- 记录超时事件用于监控

## Testing Strategy

### 1. Unit Tests
- **RouteLocator测试**: 验证路由规则的正确解析和创建
- **Filter测试**: 验证各个过滤器的功能
- **Configuration测试**: 验证配置加载和验证逻辑

### 2. Integration Tests  
- **路由转发测试**: 验证请求能正确路由到目标服务
- **熔断测试**: 验证熔断机制在服务不可用时的行为
- **配置热加载测试**: 验证配置变更能被正确应用

### 3. End-to-End Tests
- **完整请求流程测试**: 从客户端请求到下游服务响应的完整流程
- **负载测试**: 验证网关在高并发下的性能表现
- **故障恢复测试**: 验证服务恢复后的路由恢复

### 4. Contract Tests
- 使用WireMock模拟下游服务
- 验证网关与下游服务的接口契约
- 确保API兼容性

## Migration Strategy

### Phase 1: 准备阶段
- 创建新的路由配置结构
- 实现动态路由加载器
- 保持现有Feign客户端不变

### Phase 2: 并行运行
- 启用新的路由配置
- 新旧系统并行运行
- 逐步验证新路由的正确性

### Phase 3: 切换阶段  
- 禁用GatewayController中的硬编码路由
- 移除Feign客户端依赖
- 清理不必要的代码

### Phase 4: 优化阶段
- 性能调优和监控
- 完善错误处理和日志
- 文档更新和团队培训
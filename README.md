# Spring Cloud Gateway 微服务演示项目

这是一个基于Spring Cloud Gateway的微服务演示项目，展示了现代化的动态路由配置和服务治理能力。项目包含网关服务、用户服务和产品服务，采用配置驱动的路由管理方式。

## 项目结构

```
├── gateway-service/          # 网关服务 (端口8080)
├── user-service/            # 用户服务 (端口8081)
├── product-service/         # 产品服务 (端口8082)
├── eureka-service/          # 服务注册中心 (端口8761)
├── start-services.sh        # 启动脚本
├── stop-services.sh         # 停止脚本
├── test-services.sh         # 测试脚本
└── README.md               # 项目说明
```

## 技术栈

- **Spring Boot**: 2.3.2.RELEASE
- **Spring Cloud Gateway**: 2.2.4.RELEASE
- **Spring Cloud**: Hoxton.SR7
- **Circuit Breaker**: Resilience4j (替代Hystrix)
- **Java**: 8 或更高版本
- **Maven**: 3.6 或更高版本

## 环境要求

- Java 8 或更高版本
- Maven 3.6 或更高版本
- curl (用于测试脚本)

## 快速开始

### 1. 启动服务

```bash
./start-services.sh
```

启动脚本会按正确顺序启动服务：
1. 先启动User服务 (端口8081)
2. 再启动Gateway服务 (端口8080)

### 2. 测试服务

```bash
./test-services.sh
```

测试脚本会验证以下功能：
- 服务健康检查
- 直接访问User服务接口
- 通过Gateway访问User服务接口
- 错误场景处理

### 3. 停止服务

```bash
./stop-services.sh
```

## 核心特性

### 动态路由配置
- **配置驱动**: 通过YAML配置文件管理所有路由规则，无需硬编码
- **热加载**: 支持配置文件变更后动态刷新路由规则
- **路径匹配**: 支持精确匹配、前缀匹配和正则表达式匹配
- **路由优先级**: 支持路由优先级排序和启用/禁用控制

### 服务治理
- **熔断保护**: 基于Resilience4j的熔断器，替代已废弃的Hystrix
- **重试机制**: 可配置的重试策略，支持指数退避算法
- **超时控制**: 灵活的请求超时配置
- **负载均衡**: 支持多实例服务的负载均衡

### 监控和日志
- **请求日志**: 详细的请求/响应日志记录
- **性能监控**: 请求耗时和吞吐量统计
- **健康检查**: 完整的服务健康状态监控
- **指标暴露**: 通过Actuator暴露运行时指标

## 服务接口

### 用户服务接口 (通过Gateway)

- **登录接口**: `POST http://localhost:8080/user/login`
- **登出接口**: `POST http://localhost:8080/user/logout`

### 产品服务接口 (通过Gateway)

- **添加产品**: `POST http://localhost:8080/product/add`
- **查询产品**: `POST http://localhost:8080/product/query`

### 监控和管理接口

- **Gateway健康检查**: `GET http://localhost:8080/actuator/health`
- **熔断器状态**: `GET http://localhost:8080/actuator/circuitbreakers`
- **刷新配置**: `POST http://localhost:8080/actuator/refresh`

### 直接访问服务 (开发调试用)

- **User服务**: `http://localhost:8081/user/*`
- **Product服务**: `http://localhost:8082/product/*`

## 手动测试示例

```bash
# 用户服务测试
curl -X POST http://localhost:8080/user/login
curl -X POST http://localhost:8080/user/logout

# 产品服务测试
curl -X POST http://localhost:8080/product/add
curl -X POST http://localhost:8080/product/query

# 健康检查
curl http://localhost:8080/actuator/health

# 查看熔断器状态
curl http://localhost:8080/actuator/circuitbreakers

# 刷新配置 (配置变更后)
curl -X POST http://localhost:8080/actuator/refresh
```

## 故障排除

### 服务启动失败
1. 检查端口8080和8081是否被占用
2. 确认Java和Maven环境配置正确
3. 查看控制台输出的错误信息

### 测试失败
1. 确认服务已完全启动（等待更长时间）
2. 检查防火墙设置
3. 使用 `./stop-services.sh` 停止服务后重新启动

## 开发说明

- Gateway服务基于Spring Cloud Gateway实现
- User服务提供简单的REST API接口
- 所有接口返回JSON格式响应
- 包含基本的错误处理和健康检查功能
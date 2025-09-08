# 集成测试说明

## 概述

本目录包含Gateway服务的集成测试，用于验证Gateway与下游服务（用户服务和产品服务）的完整调用链。

## 测试类说明

### 1. ProductServiceEndToEndTest
- **目的**: 验证Gateway到产品服务的完整调用链
- **测试内容**:
  - 产品添加流程测试
  - 产品查询流程测试
  - 完整产品工作流测试（添加->查询）
  - Feign客户端与实际产品服务的集成测试
  - 产品服务健康检查
  - 并发产品请求测试
  - Feign客户端错误处理测试

### 2. MultiServiceCollaborationTest
- **目的**: 测试Gateway同时调用用户服务和产品服务的场景
- **测试内容**:
  - 用户服务和产品服务同时可用性测试
  - 完整业务工作流测试（用户登录->产品操作->用户登出）
  - 并发多服务请求测试
  - 服务间通信可靠性测试
  - 所有服务健康检查
  - Feign客户端负载均衡测试
  - 服务响应格式一致性测试

### 3. 现有测试类
- **GatewayIntegrationTest**: Gateway服务基础集成测试
- **EndToEndIntegrationTest**: 用户服务端到端测试

## 运行测试

### 前置条件
在运行集成测试之前，需要启动所有相关服务：

```bash
# 启动所有服务
./start-services.sh

# 或者手动启动
# 1. 启动产品服务
cd product-service && mvn spring-boot:run &

# 2. 启动用户服务  
cd user-service && mvn spring-boot:run &

# 3. 启动Gateway服务
cd gateway-service && mvn spring-boot:run &
```

### 运行端到端测试

```bash
# 运行产品服务端到端测试
mvn test -Dtest=ProductServiceEndToEndTest -De2e.test=true -f gateway-service/pom.xml

# 运行多服务协作测试
mvn test -Dtest=MultiServiceCollaborationTest -De2e.test=true -f gateway-service/pom.xml

# 运行所有端到端测试
mvn test -De2e.test=true -f gateway-service/pom.xml
```

### 运行单元测试（不需要启动服务）

```bash
# 运行所有单元测试（跳过端到端测试）
mvn test -f gateway-service/pom.xml

# 运行特定的单元测试
mvn test -Dtest=GatewayIntegrationTest -f gateway-service/pom.xml
```

## 测试配置

### 系统属性
- `e2e.test=true`: 启用端到端测试
- `test.service.down=true`: 启用服务不可用场景测试

### 服务端口
- Gateway服务: 8080
- 用户服务: 8081
- 产品服务: 8082

### 测试端点
- Gateway用户服务调用: `http://localhost:8080/gateway/user/*`
- Gateway产品服务调用: `http://localhost:8080/gateway/product/*`
- 直接用户服务调用: `http://localhost:8081/user/*`
- 直接产品服务调用: `http://localhost:8082/product/*`

## 故障排除

### 常见问题

1. **测试被跳过**
   - 确保使用了 `-De2e.test=true` 参数
   - 检查所有服务是否正常启动

2. **连接拒绝错误**
   - 验证所有服务是否在正确端口上运行
   - 检查防火墙设置
   - 确认服务启动顺序正确

3. **超时错误**
   - 检查服务响应时间
   - 调整Feign客户端超时配置
   - 验证网络连接

### 调试技巧

1. **查看服务日志**
```bash
# 查看Gateway服务日志
tail -f gateway-service/logs/application.log

# 查看产品服务日志
tail -f product-service/logs/application.log
```

2. **验证服务健康状态**
```bash
# 检查Gateway健康状态
curl http://localhost:8080/actuator/health

# 检查产品服务健康状态
curl http://localhost:8082/actuator/health

# 检查用户服务健康状态
curl http://localhost:8081/actuator/health
```

3. **手动测试API端点**
```bash
# 测试Gateway调用产品服务
curl -X POST http://localhost:8080/gateway/product/add \
  -H "Content-Type: application/json" \
  -d "{}"

# 测试Gateway调用用户服务
curl -X POST http://localhost:8080/gateway/user/login \
  -H "Content-Type: application/json" \
  -d "{}"
```

## 持续集成

在CI/CD流水线中运行这些测试时，建议：

1. 使用Docker Compose启动所有服务
2. 等待所有服务健康检查通过
3. 运行集成测试
4. 清理测试环境

示例CI配置：
```yaml
test:
  script:
    - docker-compose up -d
    - ./wait-for-services.sh
    - mvn test -De2e.test=true
    - docker-compose down
```
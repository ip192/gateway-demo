# 实施计划

- [ ] 1. 更新项目依赖和基础配置
  - 在pom.xml中添加spring-cloud-starter-openfeign和spring-boot-starter-web依赖
  - 移除spring-cloud-starter-gateway和spring-cloud-starter-circuitbreaker-reactor-resilience4j依赖
  - 移除spring-boot-starter-webflux依赖，因为将从WebFlux迁移到WebMVC
  - 更新主应用类添加@EnableFeignClients注解
  - _需求: 5.2, 5.3_

- [ ] 2. 创建Feign客户端配置类
  - 创建FeignClientConfig配置类，包含RequestInterceptor、ErrorDecoder和Retryer配置
  - 实现GatewayErrorDecoder类，处理4xx和5xx错误的不同逻辑
  - 创建自定义异常类FeignClientException和FeignServerException
  - 配置Feign的日志级别和超时设置
  - _需求: 2.1, 2.2, 3.1, 3.2_

- [ ] 3. 实现通用的Feign客户端接口
  - 创建UserServiceClient接口，支持GET、POST、PUT、DELETE方法的通用路径调用
  - 创建ProductServiceClient接口，支持GET、POST、PUT、DELETE方法的通用路径调用
  - 配置Feign客户端使用配置属性中的URL而不是硬编码
  - 为每个客户端配置fallback类和configuration类
  - _需求: 1.1, 1.2, 4.1, 4.2_

- [ ] 4. 实现Feign客户端回退处理类
  - 创建UserServiceFallback类，实现UserServiceClient接口的所有方法
  - 创建ProductServiceFallback类，实现ProductServiceClient接口的所有方法
  - 在回退方法中返回统一格式的503错误响应
  - 添加适当的日志记录，记录回退触发的原因和路径
  - _需求: 2.2, 3.3, 3.4_

- [ ] 5. 创建网关路由处理器
  - 创建GatewayRouteHandler类作为RestController，替代Spring Cloud Gateway路由
  - 实现/user/**路径的路由方法，调用UserServiceClient
  - 实现/product/**路径的路由方法，调用ProductServiceClient
  - 实现路径转换逻辑，移除网关前缀并转发到下游服务
  - 实现请求参数和请求体的完整转发逻辑
  - _需求: 1.1, 1.2, 4.1, 4.2, 4.3, 4.4_

- [ ] 6. 创建新的配置属性类
  - 创建GatewayServiceProperties类替代现有的GatewayRoutingProperties
  - 定义ServiceConfig内部类，包含url、connectTimeout、readTimeout、enabled等属性
  - 使用@ConfigurationProperties注解绑定gateway.services配置前缀
  - 移除CircuitBreakerConfig和RetryConfig相关配置，使用Feign内置配置
  - _需求: 2.1, 2.3, 5.1_

- [ ] 7. 迁移现有过滤器到拦截器模式
  - 将RequestLoggingFilter从WebFilter改为HandlerInterceptor实现
  - 将ResponseFormattingFilter从WebFilter改为HandlerInterceptor实现
  - 更新过滤器中的响应式编程代码为同步代码
  - 确保过滤器功能保持一致，包括日志记录和响应头设置
  - _需求: 6.1, 6.2_

- [ ] 8. 更新应用配置文件
  - 移除application.yml中的spring.cloud.gateway相关配置
  - 移除resilience4j相关的熔断器配置
  - 添加gateway.services配置，定义user-service和product-service的URL和超时设置
  - 添加feign.client.config配置，设置默认的连接超时、读取超时和重试配置
  - 移除management.endpoints中的circuitbreakers和circuitbreakerevents暴露
  - _需求: 2.3, 2.4, 5.1, 5.4_

- [ ] 9. 移除Resilience4j相关代码
  - 删除CircuitBreakerConfiguration类及其所有熔断器配置方法
  - 删除CircuitBreakerConfig模型类
  - 删除RetryConfig模型类
  - 更新FallbackController，移除ServerWebExchange参数，改为HttpServletRequest
  - 移除所有Mono和Flux相关的响应式编程代码
  - _需求: 5.1, 5.2, 5.3, 5.4_

- [ ] 10. 更新错误处理和响应格式
  - 修改FallbackController从WebFlux模式改为WebMVC模式
  - 确保ApiResponse格式保持一致，支持统一的错误响应结构
  - 实现全局异常处理器，处理FeignClientException和FeignServerException
  - 验证错误状态码映射正确（4xx透传，5xx返回503）
  - _需求: 3.1, 3.2, 3.3, 3.4_

- [ ] 11. 运行和修复现有测试
  - 更新所有测试类，从WebTestClient改为MockMvc
  - 修复因架构变更导致的测试失败
  - 更新测试中的响应式编程代码为同步代码
  - 验证所有端到端测试通过，确保API行为保持一致
  - 添加新的Feign客户端和回退机制的单元测试
  - _需求: 1.3, 1.4, 2.4, 3.4_

- [ ] 12. 验证服务发现支持（可选）
  - 测试Feign客户端的服务发现功能配置
  - 验证当启用服务发现时，可以通过服务名称而不是直接URL调用服务
  - 测试负载均衡功能在多实例场景下的工作情况
  - 确保服务实例不可用时的自动故障转移
  - _需求: 6.1, 6.2, 6.3, 6.4_
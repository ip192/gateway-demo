# 实施计划

- [x] 1. 设置产品服务项目结构
  - 创建product-service模块目录结构
  - 创建产品服务的pom.xml文件，复制用户服务的依赖配置
  - 更新根pom.xml添加product-service模块
  - _需求: 1.1, 1.2_

- [x] 2. 实现产品服务核心组件
  - [x] 2.1 创建ProductApplication主类
    - 编写Spring Boot应用启动类
    - 配置基本的Spring Boot注解
    - _需求: 1.1_
  
  - [x] 2.2 创建ApiResponse共享类
    - 复制用户服务的ApiResponse类到产品服务
    - 确保字段和方法与用户服务保持一致
    - _需求: 2.4, 3.4_
  
  - [x] 2.3 实现ProductController
    - 创建ProductController类，包含@RestController和@RequestMapping注解
    - 实现POST /product/add端点，返回"产品添加成功"消息
    - 实现POST /product/query端点，返回"产品查询成功"消息
    - _需求: 2.1, 2.2, 3.1, 3.2_

- [x] 3. 配置产品服务
  - [x] 3.1 创建application.properties配置文件
    - 设置服务端口为8082
    - 配置应用名称为product-service
    - 配置Actuator健康检查端点
    - _需求: 1.3_

- [x] 4. 增强Gateway服务以支持Feign客户端
  - [x] 4.1 更新Gateway服务Maven依赖
    - 在gateway-service的pom.xml中添加spring-cloud-starter-openfeign依赖
    - _需求: 5.1, 5.2_
  
  - [x] 4.2 创建Feign客户端接口
    - 创建UserServiceClient接口，定义用户服务的login和logout方法
    - 创建ProductServiceClient接口，定义产品服务的add和query方法
    - 配置@FeignClient注解指向正确的服务URL
    - _需求: 5.1, 5.2_
  
  - [x] 4.3 启用Feign客户端
    - 在GatewayApplication主类添加@EnableFeignClients注解
    - _需求: 5.1, 5.2_
  
  - [x] 4.4 创建Gateway控制器
    - 创建GatewayController类使用Feign客户端调用下游服务
    - 实现通过Feign调用用户服务和产品服务的端点
    - _需求: 5.1, 5.2_

- [x] 5. 更新Gateway路由配置
  - 在gateway-service的application.properties中添加产品服务路由配置
  - 配置产品服务路由指向http://localhost:8082
  - 添加Feign客户端超时和重试配置
  - _需求: 1.4, 5.3_

- [x] 6. 实现异常处理
  - [x] 6.1 创建产品服务异常处理器
    - 创建GlobalExceptionHandler类，复制用户服务的异常处理模式
    - 实现统一的错误响应格式
    - _需求: 4.1, 4.2, 4.3_
  
  - [x] 6.2 配置Feign错误处理
    - 实现Feign错误解码器处理下游服务错误
    - 配置服务降级机制
    - _需求: 5.4_

- [x] 7. 编写单元测试
  - [x] 7.1 创建ProductController测试
    - 编写ProductController的单元测试，测试add和query端点
    - 使用MockMvc测试HTTP请求和响应
    - _需求: 6.1, 6.2_
  
  - [x] 7.2 创建Feign客户端测试
    - 编写UserServiceClient和ProductServiceClient的测试
    - 使用WireMock模拟下游服务响应
    - _需求: 6.2_
  
  - [x] 7.3 创建异常处理测试
    - 测试产品服务的GlobalExceptionHandler
    - 测试Feign客户端的错误处理
    - _需求: 6.3_

- [x] 8. 创建集成测试
  - [x] 8.1 编写端到端集成测试
    - 创建测试类验证Gateway到产品服务的完整调用链
    - 测试Feign客户端与实际服务的集成
    - _需求: 6.2_
  
  - [x] 8.2 创建多服务协作测试
    - 测试Gateway同时调用用户服务和产品服务的场景
    - 验证服务间通信的正确性
    - _需求: 6.2_

- [x] 9. 更新部署脚本
  - [x] 9.1 更新启动脚本
    - 修改start-services.sh包含产品服务的启动命令
    - 确保正确的启动顺序
    - _需求: 1.3_
  
  - [x] 9.2 更新停止和测试脚本
    - 修改stop-services.sh包含产品服务
    - 修改test-services.sh包含产品服务的测试
    - _需求: 1.3_

- [x] 10. 验证完整系统
  - [x] 10.1 运行所有测试
    - 执行产品服务的单元测试和集成测试
    - 执行Gateway服务的Feign客户端测试
    - 确保所有测试通过
    - _需求: 6.4_
  
  - [x] 10.2 手动验证服务集成
    - 启动所有服务验证端口配置正确
    - 通过Gateway调用产品服务端点验证Feign客户端工作正常
    - 验证健康检查端点正常工作
    - _需求: 1.3, 1.4, 5.1, 5.2_
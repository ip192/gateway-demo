# 实施计划

- [x] 1. 创建专用的Info服务Feign客户端接口
  - 创建UserInfoServiceClient接口，定义/user/info端点调用
  - 创建ProductInfoServiceClient接口，定义/product/info端点调用
  - 配置Feign客户端使用InfoRoutingProperties中的URL配置
  - 为每个客户端添加相应的回退实现类
  - _需求: 1.1, 1.2, 2.2_

- [x] 2. 实现ServiceRouter组件
  - 创建ServiceRouter类，负责根据ID前缀路由到相应的Feign客户端
  - 实现routeInfoRequest方法，处理InfoRequest并调用相应的服务客户端
  - 添加基本的错误处理，将Feign异常转换为统一的响应格式
  - 集成InfoRoutingProperties用于服务路由决策
  - _需求: 1.2, 2.1, 3.1_

- [x] 3. 重构InfoRoutingFilter使用ServiceRouter
  - 移除InfoRoutingFilter中的路径修改和Gateway路由依赖逻辑
  - 添加ServiceRouter依赖并在processInfoRequest中调用
  - 简化响应处理，直接返回ServiceRouter的结果
  - 保持现有的请求验证和错误处理逻辑
  - _需求: 1.1, 1.3, 3.2_

- [x] 4. 更新Feign客户端配置
  - 修改现有的UserServiceClient和ProductServiceClient，移除硬编码URL
  - 更新Feign客户端注解使用配置属性中的URL
  - 确保所有Feign客户端使用统一的配置和错误处理
  - 验证回退机制正常工作
  - _需求: 2.2, 2.3, 3.3_

- [x] 5. 更新配置和依赖注入
  - 在InfoRoutingConfiguration中注册ServiceRouter为Bean
  - 确保所有新创建的Feign客户端和回退类正确注册为Spring组件
  - 验证InfoRoutingProperties配置正确加载
  - 测试配置开关功能正常工作
  - _需求: 2.1, 2.4_

- [x] 6. 运行和验证现有测试
  - 运行所有现有的单元测试和集成测试
  - 修复因重构导致的测试失败
  - 验证/info端点的功能与重构前保持一致
  - 确保错误处理和回退机制正常工作
  - _需求: 1.4, 3.4, 5.1, 5.3_
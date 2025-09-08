# 实施计划

- [x] 1. 创建请求数据模型
  - 创建InfoRequest类用于解析请求体中的id参数
  - 实现基本的JSON反序列化支持
  - _需求: 1.1, 1.2_

- [x] 2. 实现自定义WebFilter
  - 创建InfoRoutingFilter类实现WebFilter接口
  - 实现filter方法处理/info请求的拦截和路由逻辑
  - 添加请求体读取和JSON解析功能
  - _需求: 1.1, 1.2, 1.3, 1.4, 3.1, 3.2, 3.3_

- [x] 3. 添加路由配置属性
  - 创建InfoRoutingProperties配置类
  - 定义用户和产品服务的前缀和URL配置
  - 使用@ConfigurationProperties注解支持配置绑定
  - _需求: 5.1, 5.2, 5.3, 5.4_

- [x] 4. 实现服务路由逻辑
  - 在InfoRoutingFilter中添加id前缀匹配逻辑
  - 实现根据前缀确定目标服务URL的功能
  - 添加请求路径转换逻辑（/info -> /user/info 或 /product/info）
  - _需求: 1.1, 1.2_

- [x] 5. 集成WebFilter到Spring配置
  - 创建配置类注册InfoRoutingFilter为Bean
  - 设置适当的过滤器顺序确保在标准路由之前执行
  - 确保只对/info路径生效，其他请求使用现有路由
  - _需求: 2.1, 2.2, 2.3_

- [x] 6. 更新应用配置
  - 在application.properties中添加自定义路由配置
  - 配置用户和产品服务的前缀和URL
  - 保持现有路由配置不变
  - _需求: 5.1, 5.2, 5.3, 5.4_

- [x] 7. 更新后端服务info接口
  - 修改UserController的info方法接受id参数
  - 修改ProductController的info方法接受id参数
  - 确保接口能正确处理带参数的POST请求
  - _需求: 1.1, 1.2_

- [x] 8. 创建基本测试
  - 编写InfoRoutingFilterTest测试核心路由逻辑
  - 测试用户前缀路由到用户服务
  - 测试产品前缀路由到产品服务
  - 测试无效参数的错误处理
  - _需求: 1.1, 1.2, 1.3, 1.4_

- [x] 9. 创建集成测试
  - 编写端到端测试验证完整的路由流程
  - 测试通过网关访问/info接口的功能
  - 验证现有路由不受影响
  - _需求: 2.1, 2.2, 2.3_
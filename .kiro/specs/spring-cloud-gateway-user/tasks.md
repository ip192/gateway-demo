# 实施计划

- [x] 1. 创建项目基础结构
  - 创建根目录和两个子模块目录结构
  - 设置基本的Maven项目结构
  - _需求: 1.1, 2.1_

- [x] 2. 实现User服务基础框架
- [x] 2.1 创建User服务的Maven配置
  - 编写pom.xml文件，配置Java 8和Spring Boot依赖
  - 配置spring-boot-starter-web依赖
  - _需求: 2.2, 2.3_

- [x] 2.2 创建User服务主应用类
  - 编写UserApplication.java启动类
  - 添加@SpringBootApplication注解
  - _需求: 2.1, 2.2_

- [x] 2.3 创建User服务配置文件
  - 编写application.properties配置文件
  - 配置服务端口8081和应用名称
  - _需求: 2.2, 2.4_

- [x] 3. 实现User服务API接口
- [x] 3.1 创建响应数据模型
  - 编写ApiResponse类用于统一响应格式
  - 包含message和status字段
  - _需求: 3.2, 4.2_

- [x] 3.2 实现用户控制器
  - 编写UserController类
  - 实现/user/login POST接口，返回登录成功消息
  - 实现/user/logout POST接口，返回登出成功消息
  - _需求: 3.1, 3.2, 4.1, 4.2_

- [x] 4. 实现Gateway服务基础框架
- [x] 4.1 创建Gateway服务的Maven配置
  - 编写pom.xml文件，配置Java 8和Spring Cloud Gateway依赖
  - 配置spring-cloud-starter-gateway和spring-boot-starter-webflux依赖
  - _需求: 1.1, 1.3_

- [x] 4.2 创建Gateway服务主应用类
  - 编写GatewayApplication.java启动类
  - 添加@SpringBootApplication注解
  - _需求: 1.1, 1.2_

- [x] 4.3 创建Gateway服务路由配置
  - 编写application.properties配置文件
  - 配置服务端口8080和路由规则，将/user/**转发到User服务
  - _需求: 1.4, 3.1, 4.1_

- [x] 5. 添加错误处理和测试
- [x] 5.1 为User服务添加全局异常处理
  - 创建GlobalExceptionHandler类
  - 处理常见异常并返回标准化错误响应
  - _需求: 5.1, 5.3_

- [x] 5.2 创建基础单元测试
  - 为UserController编写单元测试
  - 测试登录和登出接口的正常响应
  - _需求: 3.4, 4.4_

- [x] 5.3 创建集成测试
  - 编写端到端测试，通过Gateway访问User服务接口
  - 验证请求转发和响应返回功能
  - _需求: 1.2, 3.1, 3.3, 4.1, 4.3_

- [x] 6. 添加健康检查和启动脚本
- [x] 6.1 配置健康检查端点
  - 为两个服务添加Spring Boot Actuator依赖
  - 配置健康检查端点
  - _需求: 5.4_

- [x] 6.2 创建启动和测试脚本
  - 创建启动脚本，按正确顺序启动服务
  - 创建测试脚本，验证接口功能
  - _需求: 1.1, 2.1_
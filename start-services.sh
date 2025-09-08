#!/bin/bash

# Spring Cloud Gateway + User Service + Product Service 启动脚本
# 按正确顺序启动服务：先启动Product服务，再启动User服务，最后启动Gateway服务

echo "=== 启动Spring Cloud微服务 ==="

# 检查Java环境
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请确保已安装Java 8或更高版本"
    exit 1
fi

# 检查Maven环境
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境，请确保已安装Maven"
    exit 1
fi

# 编译项目
echo "1. 编译项目..."
mvn clean compile -q
if [ $? -ne 0 ]; then
    echo "错误: 项目编译失败"
    exit 1
fi

# 启动Product服务
echo "2. 启动Product服务 (端口8082)..."
cd product-service
mvn spring-boot:run &
PRODUCT_SERVICE_PID=$!
cd ..

# 等待Product服务启动
echo "   等待Product服务启动..."
sleep 10

# 检查Product服务是否启动成功
if ! curl -s http://localhost:8082/actuator/health > /dev/null; then
    echo "警告: Product服务可能未完全启动，继续启动User服务..."
else
    echo "   Product服务启动成功"
fi

# 启动User服务
echo "3. 启动User服务 (端口8081)..."
cd user-service
mvn spring-boot:run &
USER_SERVICE_PID=$!
cd ..

# 等待User服务启动
echo "   等待User服务启动..."
sleep 10

# 检查User服务是否启动成功
if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo "警告: User服务可能未完全启动，继续启动Gateway服务..."
else
    echo "   User服务启动成功"
fi

# 启动Gateway服务
echo "4. 启动Gateway服务 (端口8080)..."
cd gateway-service
mvn spring-boot:run -Dmaven.test.skip=true &
GATEWAY_SERVICE_PID=$!
cd ..

# 等待Gateway服务启动
echo "   等待Gateway服务启动..."
sleep 10

# 检查Gateway服务是否启动成功
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "警告: Gateway服务可能未完全启动"
else
    echo "   Gateway服务启动成功"
fi

echo ""
echo "=== 服务启动完成 ==="
echo "Product服务: http://localhost:8082"
echo "User服务: http://localhost:8081"
echo "Gateway服务: http://localhost:8080"
echo ""
echo "进程ID:"
echo "Product服务 PID: $PRODUCT_SERVICE_PID"
echo "User服务 PID: $USER_SERVICE_PID"
echo "Gateway服务 PID: $GATEWAY_SERVICE_PID"
echo ""
echo "停止服务请运行: ./stop-services.sh"
echo "测试服务请运行: ./test-services.sh"
#!/bin/bash

# Spring Cloud Gateway + User Service + Product Service 停止脚本

echo "=== 停止Spring Cloud微服务 ==="

# 查找并停止Spring Boot应用进程
echo "1. 查找运行中的服务..."

# 停止Gateway服务
GATEWAY_PID=$(ps aux | grep 'gateway-service' | grep 'spring-boot:run' | grep -v grep | awk '{print $2}')
if [ ! -z "$GATEWAY_PID" ]; then
    echo "2. 停止Gateway服务 (PID: $GATEWAY_PID)..."
    kill $GATEWAY_PID
    sleep 3
    # 强制停止如果还在运行
    if ps -p $GATEWAY_PID > /dev/null; then
        echo "   强制停止Gateway服务..."
        kill -9 $GATEWAY_PID
    fi
    echo "   Gateway服务已停止"
else
    echo "2. Gateway服务未运行"
fi

# 停止User服务
USER_PID=$(ps aux | grep 'user-service' | grep 'spring-boot:run' | grep -v grep | awk '{print $2}')
if [ ! -z "$USER_PID" ]; then
    echo "3. 停止User服务 (PID: $USER_PID)..."
    kill $USER_PID
    sleep 3
    # 强制停止如果还在运行
    if ps -p $USER_PID > /dev/null; then
        echo "   强制停止User服务..."
        kill -9 $USER_PID
    fi
    echo "   User服务已停止"
else
    echo "3. User服务未运行"
fi

# 停止Product服务
PRODUCT_PID=$(ps aux | grep 'product-service' | grep 'spring-boot:run' | grep -v grep | awk '{print $2}')
if [ ! -z "$PRODUCT_PID" ]; then
    echo "4. 停止Product服务 (PID: $PRODUCT_PID)..."
    kill $PRODUCT_PID
    sleep 3
    # 强制停止如果还在运行
    if ps -p $PRODUCT_PID > /dev/null; then
        echo "   强制停止Product服务..."
        kill -9 $PRODUCT_PID
    fi
    echo "   Product服务已停止"
else
    echo "4. Product服务未运行"
fi

echo ""
echo "=== 所有服务已停止 ==="
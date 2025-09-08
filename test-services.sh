#!/bin/bash

# Spring Cloud Gateway + User Service + Product Service 测试脚本
# 验证接口功能

echo "=== 测试Spring Cloud微服务接口 ==="

# 检查curl命令是否可用
if ! command -v curl &> /dev/null; then
    echo "错误: 未找到curl命令，请安装curl"
    exit 1
fi

# 测试结果统计
TOTAL_TESTS=0
PASSED_TESTS=0

# 测试函数
test_endpoint() {
    local name="$1"
    local url="$2"
    local method="$3"
    local expected_status="$4"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -n "测试 $name ... "
    
    if [ "$method" = "POST" ]; then
        response=$(curl -s -w "%{http_code}" -X POST "$url" -H "Content-Type: application/json")
    else
        response=$(curl -s -w "%{http_code}" "$url")
    fi
    
    status_code="${response: -3}"
    response_body="${response%???}"
    
    if [ "$status_code" = "$expected_status" ]; then
        echo "✓ 通过 (状态码: $status_code)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        if [ ! -z "$response_body" ]; then
            echo "    响应: $response_body"
        fi
    else
        echo "✗ 失败 (期望: $expected_status, 实际: $status_code)"
        if [ ! -z "$response_body" ]; then
            echo "    响应: $response_body"
        fi
    fi
    echo ""
}

echo "1. 检查服务健康状态..."
echo ""

# 测试Product服务健康检查
test_endpoint "Product服务健康检查" "http://localhost:8082/actuator/health" "GET" "200"

# 测试User服务健康检查
test_endpoint "User服务健康检查" "http://localhost:8081/actuator/health" "GET" "200"

# 测试Gateway服务健康检查
test_endpoint "Gateway服务健康检查" "http://localhost:8080/actuator/health" "GET" "200"

echo "2. 测试直接访问Product服务接口..."
echo ""

# 直接测试Product服务添加接口
test_endpoint "Product服务添加接口" "http://localhost:8082/product/add" "POST" "200"

# 直接测试Product服务查询接口
test_endpoint "Product服务查询接口" "http://localhost:8082/product/query" "POST" "200"

echo "3. 测试直接访问User服务接口..."
echo ""

# 直接测试User服务登录接口
test_endpoint "User服务登录接口" "http://localhost:8081/user/login" "POST" "200"

# 直接测试User服务登出接口
test_endpoint "User服务登出接口" "http://localhost:8081/user/logout" "POST" "200"

echo "4. 测试通过Gateway访问Product服务接口..."
echo ""

# 通过Gateway测试产品添加接口
test_endpoint "Gateway转发产品添加请求" "http://localhost:8080/product/add" "POST" "200"

# 通过Gateway测试产品查询接口
test_endpoint "Gateway转发产品查询请求" "http://localhost:8080/product/query" "POST" "200"

echo "5. 测试通过Gateway访问User服务接口..."
echo ""

# 通过Gateway测试登录接口
test_endpoint "Gateway转发登录请求" "http://localhost:8080/user/login" "POST" "200"

# 通过Gateway测试登出接口
test_endpoint "Gateway转发登出请求" "http://localhost:8080/user/logout" "POST" "200"

echo "6. 测试错误场景..."
echo ""

# 测试不存在的路径
test_endpoint "不存在的路径" "http://localhost:8080/nonexistent" "GET" "404"

echo "=== 测试结果汇总 ==="
echo "总测试数: $TOTAL_TESTS"
echo "通过测试: $PASSED_TESTS"
echo "失败测试: $((TOTAL_TESTS - PASSED_TESTS))"

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo "✓ 所有测试通过！"
    exit 0
else
    echo "✗ 部分测试失败"
    exit 1
fi
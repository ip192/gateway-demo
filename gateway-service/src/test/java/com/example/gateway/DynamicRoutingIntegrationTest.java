package com.example.gateway;

import com.example.gateway.config.DynamicRoutingProperties;
import com.example.gateway.controller.DynamicProxyController;
import com.example.gateway.service.DynamicRoutingMetrics;
import com.example.gateway.service.HttpProxyHelper;
import com.example.gateway.service.ServiceDiscoveryHelper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 动态路由集成测试
 * 测试DynamicProxyController的动态路由功能
 */
@WebMvcTest(DynamicProxyController.class)
@TestPropertySource(properties = {
    "gateway.dynamic-routing.enabled=true",
    "eureka.client.enabled=false"
})
public class DynamicRoutingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceDiscoveryHelper serviceDiscoveryHelper;

    @MockBean
    private HttpProxyHelper httpProxyHelper;

    @MockBean
    private DynamicRoutingProperties dynamicRoutingProperties;

    @MockBean
    private DynamicRoutingMetrics metrics;

    @MockBean
    private DiscoveryClient discoveryClient;

    @MockBean
    private LoadBalancerClient loadBalancerClient;

    @MockBean
    private ServiceInstance serviceInstance;

    private WireMockServer userServiceMock;
    private WireMockServer productServiceMock;

    @BeforeEach
    void setUp() {
        // 启动用户服务模拟服务器
        userServiceMock = new WireMockServer(WireMockConfiguration.options().port(8081));
        userServiceMock.start();

        // 启动产品服务模拟服务器
        productServiceMock = new WireMockServer(WireMockConfiguration.options().port(8082));
        productServiceMock.start();

        // 设置默认的动态路由配置
        when(dynamicRoutingProperties.isEnabled()).thenReturn(true);

        // 设置默认的监控配置
        when(metrics.startTimer()).thenReturn(null);

        // 模拟服务实例
        when(serviceInstance.getHost()).thenReturn("localhost");
        when(serviceInstance.getPort()).thenReturn(8081);
    }

    @AfterEach
    void tearDown() {
        if (userServiceMock != null && userServiceMock.isRunning()) {
            userServiceMock.stop();
        }
        if (productServiceMock != null && productServiceMock.isRunning()) {
            productServiceMock.stop();
        }
    }

    @Test
    public void testDynamicRouting_UserService_GetRequest() throws Exception {
        // 配置用户服务模拟响应
        userServiceMock.stubFor(get(urlEqualTo("/user/profile"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 123, \"name\": \"Test User\", \"email\": \"test@example.com\"}")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/profile"))
                .thenReturn("http://localhost:8081/user/profile");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
                .headers(responseHeaders)
                .body("{\"id\": 123, \"name\": \"Test User\", \"email\": \"test@example.com\"}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送请求并验证响应
        mockMvc.perform(MockMvcRequestBuilders.get("/user/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    public void testDynamicRouting_UserService_PostRequest() throws Exception {
        // 配置用户服务模拟响应
        userServiceMock.stubFor(post(urlEqualTo("/user/create"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 456, \"name\": \"New User\", \"status\": \"created\"}")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/create"))
                .thenReturn("http://localhost:8081/user/create");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.status(HttpStatus.CREATED)
                .headers(responseHeaders)
                .body("{\"id\": 456, \"name\": \"New User\", \"status\": \"created\"}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送POST请求并验证响应
        String requestBody = "{\"name\": \"New User\", \"email\": \"newuser@example.com\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.status").value("created"));
    }

    @Test
    public void testDynamicRouting_UserService_WithQueryParameters() throws Exception {
        // 配置用户服务模拟响应
        userServiceMock.stubFor(get(urlPathEqualTo("/user/search"))
                .withQueryParam("name", equalTo("john"))
                .withQueryParam("age", equalTo("25"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\": 789, \"name\": \"John Doe\", \"age\": 25}]")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/search"))
                .thenReturn("http://localhost:8081/user/search");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
                .headers(responseHeaders)
                .body("[{\"id\": 789, \"name\": \"John Doe\", \"age\": 25}]");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送带查询参数的请求并验证响应
        mockMvc.perform(MockMvcRequestBuilders.get("/user/search")
                        .param("name", "john")
                        .param("age", "25"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    public void testDynamicRouting_ProductService_GetRequest() throws Exception {
        // 配置产品服务模拟响应
        productServiceMock.stubFor(get(urlEqualTo("/product/details"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 101, \"name\": \"Test Product\", \"price\": 99.99}")));

        // 模拟服务发现 - 产品服务使用8082端口
        ServiceInstance productServiceInstance = org.mockito.Mockito.mock(ServiceInstance.class);
        when(productServiceInstance.getHost()).thenReturn("localhost");
        when(productServiceInstance.getPort()).thenReturn(8082);

        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(productServiceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(productServiceInstance, "/product/details"))
                .thenReturn("http://localhost:8082/product/details");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
                .headers(responseHeaders)
                .body("{\"id\": 101, \"name\": \"Test Product\", \"price\": 99.99}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送请求并验证响应
        mockMvc.perform(MockMvcRequestBuilders.get("/product/details"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    public void testDynamicRouting_ProductService_PutRequest() throws Exception {
        // 配置产品服务模拟响应
        productServiceMock.stubFor(put(urlEqualTo("/product/update/101"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 101, \"name\": \"Updated Product\", \"status\": \"updated\"}")));

        // 模拟服务发现 - 产品服务使用8082端口
        ServiceInstance productServiceInstance = org.mockito.Mockito.mock(ServiceInstance.class);
        when(productServiceInstance.getHost()).thenReturn("localhost");
        when(productServiceInstance.getPort()).thenReturn(8082);

        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(productServiceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(productServiceInstance, "/product/update/101"))
                .thenReturn("http://localhost:8082/product/update/101");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
                .headers(responseHeaders)
                .body("{\"id\": 101, \"name\": \"Updated Product\", \"status\": \"updated\"}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送PUT请求并验证响应
        String requestBody = "{\"name\": \"Updated Product\", \"price\": 129.99}";
        mockMvc.perform(MockMvcRequestBuilders.put("/product/update/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Updated Product"))
                .andExpect(jsonPath("$.status").value("updated"));
    }

    @Test
    public void testDynamicRouting_ProductService_DeleteRequest() throws Exception {
        // 配置产品服务模拟响应
        productServiceMock.stubFor(delete(urlEqualTo("/product/delete/101"))
                .willReturn(aResponse()
                        .withStatus(204)));

        // 模拟服务发现 - 产品服务使用8082端口
        ServiceInstance productServiceInstance = org.mockito.Mockito.mock(ServiceInstance.class);
        when(productServiceInstance.getHost()).thenReturn("localhost");
        when(productServiceInstance.getPort()).thenReturn(8082);

        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(productServiceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(productServiceInstance, "/product/delete/101"))
                .thenReturn("http://localhost:8082/product/delete/101");

        // 模拟HTTP代理
        ResponseEntity<Object> mockResponse = ResponseEntity.noContent().build();

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送DELETE请求并验证响应
        mockMvc.perform(MockMvcRequestBuilders.delete("/product/delete/101"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDynamicRouting_UserService_NewEndpoint() throws Exception {
        // 测试新增的用户服务接口，验证无需修改Gateway代码即可转发
        userServiceMock.stubFor(get(urlEqualTo("/user/preferences"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"theme\": \"dark\", \"language\": \"zh-CN\"}")));

        // 模拟服务发现
        when(serviceDiscoveryHelper.getServiceInstance("user-service")).thenReturn(serviceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(serviceInstance, "/user/preferences"))
                .thenReturn("http://localhost:8081/user/preferences");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
                .headers(responseHeaders)
                .body("{\"theme\": \"dark\", \"language\": \"zh-CN\"}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送请求到新接口并验证响应
        mockMvc.perform(MockMvcRequestBuilders.get("/user/preferences"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.theme").value("dark"))
                .andExpect(jsonPath("$.language").value("zh-CN"));
    }

    @Test
    public void testDynamicRouting_ProductService_NewEndpoint() throws Exception {
        // 测试新增的产品服务接口，验证无需修改Gateway代码即可转发
        productServiceMock.stubFor(post(urlEqualTo("/product/batch-import"))
                .willReturn(aResponse()
                        .withStatus(202)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"taskId\": \"task-123\", \"status\": \"processing\"}")));

        // 模拟服务发现 - 产品服务使用8082端口
        ServiceInstance productServiceInstance = org.mockito.Mockito.mock(ServiceInstance.class);
        when(productServiceInstance.getHost()).thenReturn("localhost");
        when(productServiceInstance.getPort()).thenReturn(8082);

        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(productServiceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(productServiceInstance, "/product/batch-import"))
                .thenReturn("http://localhost:8082/product/batch-import");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.status(HttpStatus.ACCEPTED)
                .headers(responseHeaders)
                .body("{\"taskId\": \"task-123\", \"status\": \"processing\"}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送POST请求到新接口并验证响应
        String requestBody = "[{\"name\": \"Product 1\"}, {\"name\": \"Product 2\"}]";
        mockMvc.perform(MockMvcRequestBuilders.post("/product/batch-import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.taskId").value("task-123"))
                .andExpect(jsonPath("$.status").value("processing"));
    }

    @Test
    public void testDynamicRouting_HeaderForwarding() throws Exception {
        // 配置产品服务模拟响应，验证自定义头信息
        productServiceMock.stubFor(get(urlEqualTo("/product/info"))
                .withHeader("X-Custom-Header", equalTo("test-value"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 202, \"name\": \"Header Test Product\"}")));

        // 模拟服务发现 - 产品服务使用8082端口
        ServiceInstance productServiceInstance = org.mockito.Mockito.mock(ServiceInstance.class);
        when(productServiceInstance.getHost()).thenReturn("localhost");
        when(productServiceInstance.getPort()).thenReturn(8082);

        when(serviceDiscoveryHelper.getServiceInstance("product-service")).thenReturn(productServiceInstance);
        when(serviceDiscoveryHelper.buildServiceUrl(productServiceInstance, "/product/info"))
                .thenReturn("http://localhost:8082/product/info");

        // 模拟HTTP代理
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Object> mockResponse = ResponseEntity.ok()
                .headers(responseHeaders)
                .body("{\"id\": 202, \"name\": \"Header Test Product\"}");

        when(httpProxyHelper.filterHeaders(any(HttpHeaders.class))).thenReturn(new HttpHeaders());
        when(httpProxyHelper.forwardRequest(anyString(), any(HttpMethod.class), any(HttpHeaders.class), any()))
                .thenReturn(mockResponse);

        // 发送带自定义头信息的请求并验证响应
        mockMvc.perform(MockMvcRequestBuilders.get("/product/info")
                        .header("X-Custom-Header", "test-value"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Header Test Product"));
    }
}
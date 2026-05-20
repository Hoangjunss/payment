
---
name: payment-platform-api-gateway
description: Spring Cloud Gateway with Eureka integration for routing
version: 2.0
---

# Skill: Create API Gateway with Eureka

## Step 1: Dependencies
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```
Step 2: Application.yml
```yaml
spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
      routes:
        - id: payment-service
          uri: lb://payment-service
          predicates:
            - Path=/payments/**
        - id: pg-service
          uri: lb://pg-service
          predicates:
            - Path=/pg/**
eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka
```
Step 3: Main Class
```java
@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```
Step 4: Custom Global Filter (optional, for logging)
```java
@Component
@Slf4j
public class LoggingFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("Request: {} {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI());
        return chain.filter(exchange);
    }
}
```
Testing
Register gateway in Eureka.

Verify that requests to /payments/** are routed to payment-service.

Verify that requests to /pg/** are routed to pg-service.

---
name: payment-platform-eureka
description: Eureka server for service discovery (payment-platform Phase 6)
version: 2.0
---

# Skill: Create Eureka Server

## Step 1: Dependencies
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```
Step 2: Main Class
```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```
Step 3: Application.yml
```yaml
server:
  port: 8761

eureka:
  client:
    register-with-eureka: false
    fetch-registry: false
  server:
    wait-time-in-ms-when-sync-empty: 0
```
Step 4: Client Side (for each microservice)
```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
  instance:
    prefer-ip-address: true
    ```
Testing
Start the server.

Verify it's accessible at http://localhost:8761.

Register services (payment-service, pg-service) with Eureka and verify they appear in the dashboard.

Pitfalls
Setting register-with-eureka: true → infinite loop.

No eureka.client.service-url configured → can't find other services.

Missing @EnableEurekaServer → not a Eureka server.
```

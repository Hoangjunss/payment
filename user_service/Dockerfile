FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

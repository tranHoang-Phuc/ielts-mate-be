# 1) Build stage nằm ở project cha
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml ./
COPY common-library common-library
COPY identity-service identity-service
COPY notification-service notification-service
COPY discovery-service discovery-service
COPY api-gateway api-gateway
COPY file-service file-service
COPY reading-service reading-service
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-alpine AS gateway-runtime
WORKDIR /app
COPY --from=builder /workspace/api-gateway/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]


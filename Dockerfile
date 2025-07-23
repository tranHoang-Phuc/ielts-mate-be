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
COPY listening-service listening-service
COPY personal-service personal-service
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:21-alpine AS gateway-runtime
WORKDIR /app
COPY --from=builder /workspace/api-gateway/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 3) Runtime: Discovery Service
FROM eclipse-temurin:21-alpine AS discovery-runtime
WORKDIR /app
COPY --from=builder /workspace/discovery-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 4) Runtime: Identity Service
FROM eclipse-temurin:21-alpine AS identity-runtime
WORKDIR /app
COPY --from=builder /workspace/identity-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 5) Runtime: Notification Service
FROM eclipse-temurin:21-alpine AS notification-runtime
WORKDIR /app
COPY --from=builder /workspace/notification-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 6) Runtime: File Service
FROM eclipse-temurin:21-alpine AS file-runtime
WORKDIR /app
COPY --from=builder /workspace/file-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 7) Runtime: Reading Service
FROM eclipse-temurin:21-alpine AS reading-runtime
WORKDIR /app
COPY --from=builder /workspace/reading-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 8) Runtime: Listening Service
FROM eclipse-temurin:21-alpine AS listening-runtime
WORKDIR /app
COPY --from=builder /workspace/listening-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

# 9) Runtime: Personal Service
FROM eclipse-temurin:21-alpine AS personal-runtime
WORKDIR /app
COPY --from=builder /workspace/personal-service/target/*.jar app.jar
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

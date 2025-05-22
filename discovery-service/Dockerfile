FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common-library/pom.xml common-library/
COPY discovery-service/pom.xml discovery-service/
COPY . .
RUN mvn clean package -pl discovery-service -am -DskipTests

FROM eclipse-temurin:21-alpine
WORKDIR /app
COPY --from=builder /workspace/discovery-service/target/*.jar app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
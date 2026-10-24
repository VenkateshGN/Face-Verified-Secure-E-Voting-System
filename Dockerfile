# Multi-stage Dockerfile for Face-Verified Voting System
# Stage 1: Build JAR using Eclipse Temurin Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal Production JRE 21 Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create persistent storage directories
RUN mkdir -p images captured_faces keys data

COPY --from=builder /app/target/face-verified-voting-system-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]

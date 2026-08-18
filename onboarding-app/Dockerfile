# Multi-stage Dockerfile for Employee Onboarding Application
# Stage 1: Build the application
FROM maven:3.8.6-openjdk-11-slim AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Stage 2: Create the runtime image
FROM gcr.io/distroless/java11-debian11:nonroot

LABEL org.opencontainers.image.source="https://github.com/IntegrationDesigners/wxO-Demo-HR-Onboarding" \
      org.opencontainers.image.description="Employee Onboarding Application"

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar /app/application.jar

# Expose application port
EXPOSE 8080

# Run as non-root user (distroless already uses nonroot user)
USER nonroot:nonroot

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/application.jar"]
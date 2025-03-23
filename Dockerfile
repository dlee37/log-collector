# -------- Stage 1: Build the application --------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the app (skip tests for speed)
RUN mvn clean package -DskipTests

# -------- Stage 2: Runtime container --------
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Create /var/log for mounting or simulated logs and install curl
RUN mkdir -p /var/log && chmod 755 /var/log
RUN apk update && apk add --no-cache curl

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar log-collector-service.jar

# Expose ports
EXPOSE 8080

# Expose this port or any debugger port if running locally and want a debugger attached
# EXPOSE 5005

# Run the app with remote debugging enabled
# Run this if you are using a debugger on port 5005
# ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]

# Main "production"
ENTRYPOINT ["java", "-jar", "log-collector-service.jar"]

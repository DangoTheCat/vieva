# Stage 1: Build JAR using Maven and Temurin 17
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy dependency configuration and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build project
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as non-root user for security. The local document store must be writable
# by this user because uploaded course files are saved under /app/data/documents.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/data/documents \
    && chown -R appuser:appgroup /app/data
USER appuser

# Copy built artifact from builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

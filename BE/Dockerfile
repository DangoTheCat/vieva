# Stage 1: Build JAR using Maven and Temurin 17
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy dependency configuration and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build project
COPY src ./src
RUN mvn clean package -DskipTests -B \
    && find target -maxdepth 1 -name "*.jar" ! -name "original-*" ! -name "*.original" -exec cp {} /build/app.jar \;

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
COPY --from=builder /build/app.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD wget -qO- http://localhost:8080/api/v1/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

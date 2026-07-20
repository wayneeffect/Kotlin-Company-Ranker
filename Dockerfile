# Build Stage
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon

# Hardened Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user creation for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy installed distribution from build stage
COPY --from=build /app/build/install/kotlin-company-ranker /app

EXPOSE 8080
ENTRYPOINT ["/app/bin/kotlin-company-ranker"]

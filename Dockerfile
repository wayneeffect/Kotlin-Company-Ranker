# Build Stage
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle buildFatJar --no-daemon

# Hardened Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user creation for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/build/libs/*-all.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

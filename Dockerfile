# syntax=docker/dockerfile:1

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package

# ---- Stage 2: minimal runtime, hardened ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user — a container-security baseline that image scanners check for.
RUN useradd --system --uid 1001 --no-create-home appuser
COPY --from=build /app/target/app.jar ./app.jar
USER appuser

EXPOSE 8080

# Dependency-free health check (no curl/wget needed in the image).
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ──────────────────────────────────────────────────────────────
# Stage 1: Build 
# ──────────────────────────────────────────────────────────────
FROM maven:3.8.5-openjdk-17 AS builder
WORKDIR /workspace

# Copy pom.xml and source code
COPY pom.xml .
COPY src src

# Build the fat JAR (skip test compilation and execution)
RUN mvn package -Dmaven.test.skip=true -q

# ──────────────────────────────────────────────────────────────
# Stage 2: Runtime  (uses a tiny JRE)
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the compiled JAR
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

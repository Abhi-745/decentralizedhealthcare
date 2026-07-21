# ──────────────────────────────────────────────────────────────
# Stage 1: Build  (uses the full JDK to compile)
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper first (changes rarely → cached layer)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

# Download all dependencies into the image (cached separately)
RUN ./mvnw dependency:go-offline -q

# Copy source code and build the fat JAR (skip tests — already run in CI)
COPY src src
RUN ./mvnw package -DskipTests -q

# ──────────────────────────────────────────────────────────────
# Stage 2: Runtime  (uses a tiny JRE — no compiler, no tools)
# ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Only copy the compiled JAR from Stage 1 — not source code
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

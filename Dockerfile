# ── Stage 1: Build ──────────────────────────────────────────
# Use Java 21 to match pom.xml <java.version>21</java.version>
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml first
# Docker caches this layer — re-downloads only if pom.xml changes
COPY pom.xml .

# Download all Maven dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the executable JAR
# -DskipTests  → skip tests during build (run separately)
# -B           → batch mode for cleaner logs
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ────────────────────────────────────────────
# Use slim JRE image — no Maven or source code in final image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy ONLY the JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Document which port the app listens on
EXPOSE 8080

# Start command
# -Dspring.profiles.active=prod  → use application-prod.yml
# -Djava.security.egd=...        → faster startup on Linux
ENTRYPOINT ["java", \
            "-Dspring.profiles.active=prod", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", \
            "app.jar"]
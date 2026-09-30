# =========================
# Stage 1: Build
# =========================
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy pom first for better Docker layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# =========================
# Stage 2: Run
# =========================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy the generated JAR from the builder
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot default port
EXPOSE 10000

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
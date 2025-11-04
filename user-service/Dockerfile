FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY common common
COPY schema schema
COPY user-service user-service

# Build the application
RUN chmod +x gradlew && ./gradlew :user-service:build -x test

FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built artifact from builder
COPY --from=builder /app/user-service/build/libs/*.jar app.jar

# Expose port
EXPOSE 8085

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

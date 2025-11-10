FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy common library first
COPY common/ /app/common/
COPY settings.gradle /app/
COPY build.gradle /app/

# Copy analytics-service
COPY analytics-service/build.gradle /app/analytics-service/
COPY analytics-service/src /app/analytics-service/src

# Build
RUN gradle :analytics-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/analytics-service/build/libs/*.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]

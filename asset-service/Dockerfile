FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy common library first
COPY common/ /app/common/
COPY settings.gradle /app/
COPY build.gradle /app/

# Copy asset-service
COPY asset-service/build.gradle /app/asset-service/
COPY asset-service/src /app/asset-service/src

# Build
RUN gradle :asset-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/asset-service/build/libs/*.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]

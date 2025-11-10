FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy common library first
COPY common/ /app/common/
COPY settings.gradle /app/
COPY build.gradle /app/

# Copy investment-service
COPY investment-service/build.gradle /app/investment-service/
COPY investment-service/src /app/investment-service/src

# Build
RUN gradle :investment-service:build -x test --no-daemon

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/investment-service/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]

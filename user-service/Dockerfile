# Stage 1: Build JAR
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Run app
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENTRYPOINT ["java",
            "-XX:+UseContainerSupport",
            "-XX:MaxRAMPercentage=75.0",
            "-jar", "app.jar"]
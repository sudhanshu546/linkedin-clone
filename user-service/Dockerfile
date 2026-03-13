FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# Copy the entire project so Maven reactor works correctly
COPY . .
# Build the project, only producing the JAR for the specific service
RUN mvn clean package -DskipTests -pl user-service -am

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
# Extract the jar from the correct subfolder
COPY --from=build /app/user-service/target/user-service-*.jar app.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

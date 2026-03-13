FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
# Copy the entire project so Maven reactor works correctly
COPY . .
# Build only the specific module and its dependencies
RUN mvn clean package -DskipTests -pl job-service -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Extract the jar from the correct module folder
COPY --from=build /build/job-service/target/job-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

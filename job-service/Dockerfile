FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# Copy only this service's folder and the common folders
COPY common-models /app/common-models
COPY common-utility /app/common-utility
COPY job-service /app/job-service
COPY pom.xml /app/pom.xml 

# Build the common modules first, then the service
RUN mvn install -DskipTests -pl common-models,common-utility -am
RUN mvn package -DskipTests -pl job-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/job-service/target/job-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

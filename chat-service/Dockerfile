FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
# Copy only this service's folder and the common folders
COPY common-models /app/common-models
COPY common-utility /app/common-utility
COPY chat-service /app/chat-service
COPY pom.xml /app/pom.xml 

# Build the common modules first, then the service
RUN mvn install -DskipTests -pl common-models,common-utility -am
RUN mvn package -DskipTests -pl chat-service

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/chat-service/target/chat-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

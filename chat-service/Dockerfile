# Use a lightweight JRE for the final image
FROM eclipse-temurin:17-jre-alpine

# Add a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app

# Copy the jar from the target directory
COPY target/chat-service-*.jar app.jar

# Optimize JVM settings for container environments
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]

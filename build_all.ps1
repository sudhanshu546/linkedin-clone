# Build and install common modules
cd common-models
mvn clean install -DskipTests
cd ..

cd common-utility
mvn clean install -DskipTests
cd ..

# Build all other services
$services = "discovery-service", "api-gateway", "user-service", "profile-service", "notification-service", "job-service", "chat-service"

foreach ($service in $services) {
    echo "Building $service..."
    cd $service
    mvn clean package -DskipTests
    cd ..
}

echo "All services built successfully!"

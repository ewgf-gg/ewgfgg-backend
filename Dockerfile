FROM eclipse-temurin:21-jre-alpine

# Create app directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Environment variables (these will be overridden in production)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
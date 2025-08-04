# Build stage
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy only what we need for this service
COPY build.gradle ./
COPY src ./src

# Create a minimal settings.gradle if it doesn't exist
RUN echo "rootProject.name = 'novel-memory-service'" > settings.gradle

# Build the JAR
RUN gradle build --no-daemon -x test

# Runtime stage  
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# Create non-root user
RUN addgroup -g 1001 -S appuser && \
    adduser -S appuser -u 1001 -G appuser && \
    chown -R appuser:appuser /app

USER appuser

# Expose port 8080 (Cloud Run default)
EXPOSE 8080

# Start the application
CMD ["java", "-cp", "app.jar", "com.deusexmachina.novel.memory.CloudRunServer"]
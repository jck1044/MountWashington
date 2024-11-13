# Use OpenJDK base image
FROM openjdk:11-jre-slim

# Install netcat
RUN apt-get update && apt-get install -y netcat && rm -rf /var/lib/apt/lists/*

# Create an app directory in the container
WORKDIR /app

# Copy the built .jar files for each application into the container
COPY data-fetcher/build/libs/data-fetcher.jar /app/data-fetcher.jar
COPY weather-stream-app/build/libs/weather-stream-app-1.0.jar /app/weather-stream-app.jar

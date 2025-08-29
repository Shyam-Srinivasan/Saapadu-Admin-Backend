FROM ubuntu:latest
LABEL authors="SHYAM"

ENTRYPOINT ["top", "-b"]
# Stage 1: Build the application using Maven
# Use the appropriate base image for your Java version (e.g., openjdk:17)
FROM maven:3.9.8-eclipse-temurin-24 AS build

WORKDIR /app

# Copy the pom.xml and download dependencies to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Build the application, skipping tests for a faster CI/CD build
RUN mvn clean package -DskipTests

# Stage 2: Create the final, smaller production image
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copy the built JAR file from the 'build' stage
COPY --from=build /app/target/*.jar app.jar
# The PORT environment variable is set by Render. Your app will listen on this port.
ENTRYPOINT ["java", "-jar", "app.jar"]

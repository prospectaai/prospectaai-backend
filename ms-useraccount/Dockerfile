# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the Maven project files
COPY pom.xml .
COPY src ./src

# Build the Spring Boot application
RUN mvn clean package -DskipTests

# Stage 2: Create the final image
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port your Spring Boot application runs on (default is 8080)
EXPOSE 8080

# Command to run the Spring Boot application
CMD ["java", "-jar", "app.jar"]
# Use an official OpenJDK runtime as a parent image
FROM openjdk:18-jdk-alpine

# Set the working directory in the container
WORKDIR /app

# Copy the .jar file to the working directory in the container

COPY ./target/moObserver-2-all.jar /app/moObserver.jar
COPY ./.env .
# Expose port 8080 if your application listens on it (optional)
# EXPOSE 8080

# Define the command to run the .jar file
ENTRYPOINT ["java", "-jar", "/app/moObserver.jar"]

FROM openjdk:17-jdk-slim

WORKDIR /app

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} app.jar

# HTTP REST API port
EXPOSE 8080

# Napomena: Gateway je gRPC CLIENT (ne server), pa ne treba gRPC server port

ENTRYPOINT ["java", "-jar", "app.jar"]


FROM eclipse-temurin:11-jre-alpine

WORKDIR /app

# Copy the fat JAR produced by mvn package
COPY target/project-3-team-29-1.0-SNAPSHOT.jar app.jar

# Copy keystore so TLS works inside the container
COPY src/main/resources/keystore.jks keystore.jks

ENV OMS_KEYSTORE_PATH=/app/keystore.jks \
    OMS_KEYSTORE_PASS=changeit

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

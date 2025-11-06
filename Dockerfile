# -------- Build stage: creates the JAR
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies first
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# Now copy sources and build
COPY src ./src
RUN mvn -q -DskipTests clean package

# -------- Run stage: minimal, secure base
FROM gcr.io/distroless/java21:nonroot
WORKDIR /app

# Copy the built jar and rename to app.jar (works for any jar name)
COPY --from=build /workspace/target/*.jar /app/app.jar

# Reasonable JVM limits for Cloud Run
#ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Cloud Run provides $PORT; your app already reads it
EXPOSE 8080
#USER nonroot

# Use prod profile in Cloud Run
ENTRYPOINT ["java","-jar","/app/app.jar","--spring.profiles.active=prod"]

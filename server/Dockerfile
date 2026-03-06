# Build stage: compile and package everything with Gradle
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew :server:fatJar --no-daemon

# Runtime stage: minimal image with only the fat JAR
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/server/build/libs/coderive-server.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]

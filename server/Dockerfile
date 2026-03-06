FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .
RUN javac -cp "lib/*" PlaygroundServer.java
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

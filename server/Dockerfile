FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY server/ .

# Wildcard includes ALL jars in lib/
RUN javac -cp "lib/*" PlaygroundServer.java

# Wildcard works at runtime too!
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

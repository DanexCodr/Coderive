FROM eclipse-temurin:17-jdk
WORKDIR /app
# Copy Java file and lib folder
COPY PlaygroundServer.java .
COPY lib/ lib/
# Compile with spark in classpath
RUN javac -cp "lib/*" PlaygroundServer.java
# Run with spark in classpath
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

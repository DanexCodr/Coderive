FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY . .

# DEBUG: See what we have
RUN echo "=== FILES ===" && ls -la
RUN echo "=== LIB ===" && ls -la lib/

# Compile
RUN javac -cp "lib/*" PlaygroundServer.java

# DEBUG: Verify compilation
RUN echo "=== CLASSES ===" && ls -la *.class

# Run
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

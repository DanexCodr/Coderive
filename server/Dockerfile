FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy ONLY the server folder contents
COPY server/ .

# Now PlaygroundServer.java is in /app/
RUN ls -la

# Compile
RUN javac -cp "lib/*" PlaygroundServer.java

CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

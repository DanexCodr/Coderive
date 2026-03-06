FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy everything
COPY . .

# Verify
RUN ls -la /app/

# Compile
RUN javac -cp "lib/*" PlaygroundServer.java

CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

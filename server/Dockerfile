FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy with explicit path
COPY PlaygroundServer.java /app/
COPY lib/ /app/lib/

# Verify
RUN ls -la /app/

# Compile
RUN javac -cp "lib/*" PlaygroundServer.java

CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

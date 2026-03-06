FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy your server files
COPY PlaygroundServer.java .
COPY lib/ lib/

# Download Coderive.jar from GitHub Pages (CORRECT URL)
ADD https://danexcodr.github.io/Coderive/assets/Coderive.jar /app/Coderive.jar

# Compile with both JARs
RUN javac -cp "Coderive.jar:lib/*" PlaygroundServer.java

# Run with both JARs
CMD ["java", "-cp", ".:Coderive.jar:lib/*", "PlaygroundServer"]

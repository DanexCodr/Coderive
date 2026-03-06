FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy EVERYTHING from server/ (including lib/ with Spark)
COPY . .

# Download Coderive.jar (still need this)
ADD https://danexcodr.github.io/Coderive/assets/Coderive.jar /app/Coderive.jar

# Compile - Spark is already in lib/
RUN javac -cp "Coderive.jar:lib/*" PlaygroundServer.java

# Run
CMD ["java", "-cp", ".:Coderive.jar:lib/*", "PlaygroundServer"]

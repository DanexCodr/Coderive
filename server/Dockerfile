FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy everything from server/
COPY . .

# lib/ now contains both:
# - spark-core-2.9.4.jar
# - Coderive.jar
# Compile with all JARs in lib/
RUN javac -cp "lib/*" PlaygroundServer.java

# Run with all JARs in classpath
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

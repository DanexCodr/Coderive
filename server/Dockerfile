FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY server/*.java .
COPY server/lib/* lib/
RUN javac -cp "lib/*" *.java
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

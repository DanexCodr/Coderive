FROM openjdk:7-jdk-alpine
WORKDIR /app
COPY server/*.java .
COPY server/lib/* lib/
RUN javac -cp "lib/*" *.java
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

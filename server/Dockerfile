FROM eclipse-temurin:7
WORKDIR /app
COPY server/*.java .
COPY server/lib/* lib/
RUN javac -cp "lib/*" *.java
CMD ["java", "-cp", ".:lib/*", "PlaygroundServer"]

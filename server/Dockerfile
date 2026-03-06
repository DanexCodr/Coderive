FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY server/ .

RUN javac -cp "lib/spark-core-2.9.4.jar:lib/Coderive.jar:lib/slf4j-simple-1.7.36.jar" PlaygroundServer.java

CMD ["java", "-cp", ".:lib/spark-core-2.9.4.jar:lib/Coderive.jar:lib/slf4j-simple-1.7.36.jar", "PlaygroundServer"]

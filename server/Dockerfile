FROM eclipse-temurin:17-jdk
WORKDIR /app

# Explicit paths - CLEAR what goes where
COPY PlaygroundServer.java /app/
COPY lib/ /app/lib/

ADD https://danexcodr.github.io/Coderive/assets/Coderive.jar /app/Coderive.jar

RUN javac -cp "Coderive.jar:lib/*" PlaygroundServer.java

CMD ["java", "-cp", ".:Coderive.jar:lib/*", "PlaygroundServer"]

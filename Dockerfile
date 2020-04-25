FROM openjdk:8-jdk-alpine

ENV RUNTIME java8
ENV JAR_PATH /tmp/index.jar
ENV FUNC_NAME ""

COPY ./target/Env-java-1.0-SNAPSHOT-jar-with-dependencies.jar /app.jar

ENTRYPOINT [ "java", "-jar", "/app.jar" ]
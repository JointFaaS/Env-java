FROM openjdk:8-jdk-alpine
COPY ./target/Env-java-1.0-SNAPSHOT-jar-with-dependencies.jar /app.jar
ENTRYPOINT [ "java", "-jar", "/app.jar" ]
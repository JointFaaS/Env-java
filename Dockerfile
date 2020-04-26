FROM maven:3-jdk-8 AS build

WORKDIR /tmp
COPY . .
RUN mvn package -Dmaven.test.skip=true

FROM openjdk:8-jdk-alpine

ENV RUNTIME java8
ENV JAR_PATH /tmp/index.jar
ENV FUNC_NAME ""

COPY --from=build /tmp/target/Env-java-1.0-SNAPSHOT-jar-with-dependencies.jar /app.jar

ENTRYPOINT [ "java", "-jar", "/app.jar" ]

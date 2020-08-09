FROM openjdk:14.0.2-slim-buster
MAINTAINER Fayzelgayanov Marat <f4815162342@gmail.com>
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
VOLUME /dist
ENTRYPOINT ["java", "-jar", "app.jar"]
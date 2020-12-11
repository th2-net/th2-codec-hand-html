FROM gradle:6.6-jdk11 AS build
ARG app_version=0.0.0
COPY ./ .
RUN gradle build -Prelease_version=${app_version}

RUN mkdir /home/app
RUN cp ./build/libs/*.jar /home/app/application.jar

FROM openjdk:12-alpine
COPY --from=build /home/app /home/app

WORKDIR /home/app/
ENTRYPOINT ["java", "-jar", "/home/app/application.jar"]
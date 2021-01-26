FROM gradle:6.6-jdk11 AS build
ARG release_version
COPY ./ .
RUN gradle build -Prelease_version=${release_version}

RUN mkdir /home/app
RUN cp ./build/libs/*.jar /home/app/application.jar

FROM adoptopenjdk/openjdk11:alpine
COPY --from=build /home/app /home/app

WORKDIR /home/app/
ENTRYPOINT ["java", "-jar", "/home/app/application.jar"]
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
RUN mvn -B verify
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system depor && useradd --system --gid depor depor
COPY --from=build /app/target/depor-hub-1.0.0.jar app.jar
USER depor
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM eclipse-temurin:17-jdk AS build

ARG GITHUB_TOKEN
ARG PORT

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -s .mvn/settings-docker.xml dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw -s .mvn/settings-docker.xml clean package -DskipTests

FROM eclipse-temurin:17-jre

ARG PORT=8084

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE ${PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]

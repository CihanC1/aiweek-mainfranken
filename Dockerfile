FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src src
COPY event-sources.csv .
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /work
COPY --from=build /workspace/target/quarkus-app/ ./
COPY --from=build /workspace/event-sources.csv ./
EXPOSE 8080
USER 1001
ENTRYPOINT ["java","-Dquarkus.profile=docker","-jar","quarkus-run.jar"]

FROM maven:3.9.16-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/philterd-policy-editor.jar philterd-policy-editor.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "philterd-policy-editor.jar"]

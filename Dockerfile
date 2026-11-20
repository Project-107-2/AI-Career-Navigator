FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /project
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /project/target/ai-career-navigator-1.0-SNAPSHOT.jar app.jar
EXPOSE 4567
CMD ["java", "-jar", "app.jar"]

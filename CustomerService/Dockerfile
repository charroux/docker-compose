FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

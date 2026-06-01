# Build the Spring Boot fat jar with JDK 24 (matches the Gradle toolchain) using the project wrapper.
FROM eclipse-temurin:24-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
# Railway injects PORT; Spring binds it via server.port=${PORT:8083}.
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]

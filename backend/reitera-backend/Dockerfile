# Stage 1 — compile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Stage 2 — run (smaller image, no compiler)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/reitera-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

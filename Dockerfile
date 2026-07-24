# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Cache dependencies separately from source so rebuilds are fast
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ===== Run stage =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/dhobigo-backend.jar app.jar

EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]

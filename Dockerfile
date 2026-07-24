# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ===== Runtime =====
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/dhobigo-backend.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
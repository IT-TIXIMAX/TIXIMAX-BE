# Stage 1: Build
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

COPY pom.xml .
# Download dependencies for offline use to cache them
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
# Using JRE alpine version to minimize the final image size
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/txm-0.0.1-SNAPSHOT.jar project.jar

ENTRYPOINT ["java", "-jar", "project.jar"]

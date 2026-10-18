# Stage 1: build the jar inside Docker (no local mvn package needed)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# Stage 2: runtime image with Java 17 JRE
FROM eclipse-temurin:17-jre
VOLUME /tmp
COPY --from=build /build/target/app.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]

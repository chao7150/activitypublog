# Build stage
FROM gradle:8.5-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew build -x test --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Timezone
ENV TZ=Asia/Tokyo

ENTRYPOINT ["java", "-jar", "app.jar"]

# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle build.gradle ./
COPY popngg-domain popngg-domain
COPY popngg-application popngg-application
COPY popngg-infra popngg-infra
COPY popngg-api popngg-api
RUN ./gradlew :popngg-api:bootJar --no-daemon

FROM eclipse-temurin:21-jre
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --create-home popngg
WORKDIR /app
COPY --from=build --chown=popngg:popngg \
  /workspace/popngg-api/build/libs/popngg-api-*.jar app.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD ["curl", "--fail", "--silent", "http://127.0.0.1:8080/actuator/health"]
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]

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

FROM eclipse-temurin:21-jre-noble
ARG POPNGG_RELEASE_VERSION=local
ARG POPNGG_GIT_SHA=unknown
ARG POPNGG_BUILD_TIME=unknown
ENV POPNGG_RELEASE_VERSION=${POPNGG_RELEASE_VERSION} \
    POPNGG_GIT_SHA=${POPNGG_GIT_SHA} \
    POPNGG_BUILD_TIME=${POPNGG_BUILD_TIME} \
    ACHIEVEMENT_PYTHON=/opt/achievement/bin/python \
    ACHIEVEMENT_SCRIPT=/app/analysis/achievement/experiment.py
RUN apt-get update \
  && apt-get install -y --no-install-recommends curl fonts-noto-cjk python3 python3-venv \
  && rm -rf /var/lib/apt/lists/* \
  && useradd --system --uid 10001 --create-home popngg
COPY analysis/achievement/requirements-runtime.txt /tmp/achievement-requirements.txt
RUN python3 -m venv /opt/achievement \
  && /opt/achievement/bin/pip install --no-cache-dir --only-binary=:all: -r /tmp/achievement-requirements.txt \
  && /opt/achievement/bin/python -c "import numpy, scipy; from scipy.optimize import minimize" \
  && rm /tmp/achievement-requirements.txt
WORKDIR /app
COPY --from=build --chown=popngg:popngg \
  /workspace/popngg-api/build/libs/popngg-api-*.jar app.jar
COPY --chown=popngg:popngg analysis/achievement/experiment.py /app/analysis/achievement/experiment.py
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD ["curl", "--fail", "--silent", "http://127.0.0.1:8080/health"]
# Reserve memory for the sequential achievement-model Python process and native JVM overhead.
ENTRYPOINT ["java", "-Djava.awt.headless=true", "-XX:MaxRAMPercentage=50", "-jar", "/app/app.jar"]

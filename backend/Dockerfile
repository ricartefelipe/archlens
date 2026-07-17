FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY contracts/analysis.schema.json backend/src/main/resources/analysis.schema.json
WORKDIR /workspace/backend
RUN mvn -B -DskipTests package \
    && test -f target/quarkus-app/quarkus-run.jar

FROM eclipse-temurin:21.0.6_7-jre-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 archlens \
    && useradd --system --uid 10001 --gid archlens --home-dir /app --shell /usr/sbin/nologin archlens
COPY --from=build --chown=archlens:archlens /workspace/backend/target/quarkus-app /app
USER 10001:10001
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=12 \
  CMD curl -fsS http://127.0.0.1:8080/q/health/ready || exit 1
CMD ["java", "-jar", "quarkus-run.jar"]

FROM maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY contracts/analysis.schema.json backend/src/main/resources/analysis.schema.json
WORKDIR /workspace/backend
RUN mvn -B -DskipTests package \
    && test -f target/quarkus-app/quarkus-run.jar

FROM eclipse-temurin:21.0.13_9-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269
WORKDIR /app
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
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

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY contracts/analysis.schema.json backend/src/main/resources/analysis.schema.json
WORKDIR /workspace/backend
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/backend/target/quarkus-app /app
ENV JAVA_OPTS="-Dquarkus.http.host=0.0.0.0"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD curl -fsS http://127.0.0.1:8080/q/health/ready || exit 1
CMD ["java", "-jar", "quarkus-run.jar"]

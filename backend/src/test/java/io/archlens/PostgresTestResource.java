package io.archlens;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * PostgreSQL real via Testcontainers para a suíte Quarkus.
 * Se Docker Desktop estiver inativo: {@code DOCKER_HOST=unix:///var/run/docker.sock}.
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("archlens")
            .withUsername("archlens")
            .withPassword("archlens");

    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        Map<String, String> config = new HashMap<>();
        config.put("quarkus.datasource.db-kind", "postgresql");
        config.put("quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl());
        config.put("quarkus.datasource.username", POSTGRES.getUsername());
        config.put("quarkus.datasource.password", POSTGRES.getPassword());
        config.put("quarkus.liquibase.migrate-at-start", "true");
        config.put("quarkus.liquibase.change-log", "db/changelog/db.changelog-postgresql.yaml");
        config.put("quarkus.hibernate-orm.schema-management.strategy", "none");
        return config;
    }

    @Override
    public void stop() {
        POSTGRES.stop();
    }
}

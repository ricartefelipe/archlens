package io.archlens;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class LiquibaseMigrationIT {

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String jdbcUrl;

    @ConfigProperty(name = "quarkus.datasource.username")
    String username;

    @ConfigProperty(name = "quarkus.datasource.password")
    String password;

    @Test
    void migrationsAppliedAndIdempotentAgainstPostgreSQL() throws Exception {
        assertTrue(jdbcUrl.contains("postgresql") || jdbcUrl.contains("postgres"),
                "Esperado PostgreSQL real, URL=" + jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            assertTrue(tableExists(conn, "organization"));
            assertTrue(tableExists(conn, "assessment"));
            assertTrue(tableExists(conn, "finding"));
            assertTrue(tableExists(conn, "report_snapshot"));
            assertTrue(tableExists(conn, "inventory_module"));
            assertTrue(columnExists(conn, "inventory_module", "organization_id"));
            assertTrue(columnExists(conn, "finding", "origin"));
            assertTrue(columnExists(conn, "finding", "certainty"));
            assertTrue(indexExists(conn, "idx_finding_org_assessment"));
        }

        // Segunda/terceira execução em conexões novas — prova idempotência Liquibase
        runUpdate();
        runUpdate();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             ResultSet rs = conn.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM databasechangelog WHERE id LIKE '00%'")) {
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 2, "Deve haver ao menos 2 changesets registrados");
        }
    }

    private void runUpdate() throws Exception {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-postgresql.yaml",
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.update("");
            }
        }
    }

    private static boolean tableExists(Connection conn, String table) throws Exception {
        try (ResultSet rs = conn.getMetaData().getTables(null, "public", table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (ResultSet rs = conn.getMetaData().getColumns(null, "public", table, column)) {
            return rs.next();
        }
    }

    private static boolean indexExists(Connection conn, String index) throws Exception {
        try (var ps = conn.prepareStatement(
                "SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = ?")) {
            ps.setString(1, index);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}

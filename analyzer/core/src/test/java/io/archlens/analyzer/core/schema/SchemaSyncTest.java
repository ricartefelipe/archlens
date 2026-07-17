package io.archlens.analyzer.core.schema;

import io.archlens.analyzer.core.testsupport.WorkspacePaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaSyncTest {

    @Test
    void contractsSchemaMatchesBackendCopy() throws Exception {
        Path contracts = WorkspacePaths.analysisSchema();
        Path backend = WorkspacePaths.workspaceRoot().resolve("backend/src/main/resources/analysis.schema.json");
        assertTrue(Files.isRegularFile(contracts));
        assertTrue(Files.isRegularFile(backend));
        assertEquals(Files.readString(contracts), Files.readString(backend));
    }
}

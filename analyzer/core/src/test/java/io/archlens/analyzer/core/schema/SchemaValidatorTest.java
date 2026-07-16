package io.archlens.analyzer.core.schema;

import io.archlens.analyzer.core.testsupport.WorkspacePaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaValidatorTest {

    private static Path schemaPath() {
        return WorkspacePaths.analysisSchema();
    }

    @Test
    void rejectsInvalidJson() throws Exception {
        String invalid = """
                {
                  "schemaVersion": "1.0.0"
                }
                """;
        SchemaValidator.ValidationResult result = SchemaValidator.validateJson(schemaPath(), invalid);
        assertFalse(result.success());
    }
}

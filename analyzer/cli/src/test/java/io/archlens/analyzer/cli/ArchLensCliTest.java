package io.archlens.analyzer.cli;

import io.archlens.analyzer.cli.testsupport.WorkspacePaths;
import io.archlens.analyzer.core.cli.ExitCodes;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchLensCliTest {

    private static Path sampleRoot() {
        return WorkspacePaths.sampleLegacyDemo();
    }

    private static Path schemaPath() {
        return WorkspacePaths.analysisSchema();
    }

    @Test
    void versionCommandPrintsVersion() {
        CommandLine cmd = new CommandLine(new ArchLensCli());
        int exit = cmd.execute("version");
        assertEquals(ExitCodes.SUCCESS, exit);
    }

    @Test
    void scanAndValidateSample(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("analysis.json");
        CommandLine cmd = new CommandLine(new ArchLensCli());

        int scanExit = cmd.execute("scan", "--path", sampleRoot().toString(), "--out", output.toString());
        assertEquals(ExitCodes.SUCCESS, scanExit);
        assertTrue(Files.isRegularFile(output));

        int validateExit = cmd.execute(
                "validate",
                "--file", output.toString(),
                "--schema", schemaPath().toString()
        );
        assertEquals(ExitCodes.SUCCESS, validateExit);
    }

    @Test
    void validateRejectsInvalidFile(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        Path invalid = tempDir.resolve("invalid.json");
        Files.writeString(invalid, "{\"schemaVersion\":\"1.0.0\"}");

        CommandLine cmd = new CommandLine(new ArchLensCli());
        int exit = cmd.execute(
                "validate",
                "--file", invalid.toString(),
                "--schema", schemaPath().toString()
        );
        assertEquals(ExitCodes.VALIDATION_FAILED, exit);
    }

    @Test
    void scanRejectsInvalidPath() {
        CommandLine cmd = new CommandLine(new ArchLensCli());
        int exit = cmd.execute("scan", "--path", "/no/such/archlens/path", "--out", "/tmp/x.json");
        assertEquals(ExitCodes.INVALID_INPUT, exit);
    }
}

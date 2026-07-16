package io.archlens.analyzer.core.walk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeFileWalkerTest {

    @TempDir
    Path tempDir;

    @Test
    void ignoresBuildDirectories() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("target/generated"));
        Files.writeString(tempDir.resolve("src/App.java"), "class App {}");
        Files.writeString(tempDir.resolve("target/generated/Ignored.java"), "class Ignored {}");

        List<Path> files = SafeFileWalker.walkFiles(tempDir, path -> path.toString().endsWith(".java"));
        assertEquals(1, files.size());
        assertTrue(files.getFirst().toString().endsWith("src/App.java"));
    }
}

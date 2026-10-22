package io.archlens.analyzer.core.walk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeFileWalkerTest {

    @TempDir
    Path tempDir;

    @Test
    void ignoresBuildDirectories() throws Exception {
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve("target/generated"));
        Files.createDirectories(tempDir.resolve(".vscode"));
        Files.writeString(tempDir.resolve("src/App.java"), "class App {}");
        Files.writeString(tempDir.resolve("target/generated/Ignored.java"), "class Ignored {}");
        Files.writeString(tempDir.resolve(".vscode/settings.json"), "{}");

        List<Path> files = SafeFileWalker.walkFiles(tempDir, path -> path.toString().endsWith(".java"));
        assertEquals(1, files.size());
        assertTrue(SafeFileWalker.toRelative(tempDir, files.getFirst()).endsWith("src/App.java"));
    }

    @Test
    void ignoresExternalSymlinkAndFollowsInternal(@TempDir Path outside) throws Exception {
        Path external = outside.resolve("external.txt");
        Files.writeString(external, "outside");
        Path internalDir = tempDir.resolve("inside");
        Files.createDirectories(internalDir);
        Files.writeString(internalDir.resolve("ok.java"), "class Ok {}");
        Path externalLink = tempDir.resolve("escape");
        Path internalLink = tempDir.resolve("link-inside");
        Files.createSymbolicLink(externalLink, external.getParent());
        Files.createSymbolicLink(internalLink, internalDir);

        SafeFileWalker.WalkResult result = SafeFileWalker.walk(tempDir, path -> true, WalkLimits.DEFAULT);
        List<String> relatives = result.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
        assertTrue(relatives.stream().anyMatch(r -> r.contains("ok.java")));
        assertFalse(relatives.stream().anyMatch(r -> r.contains("external.txt")));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.toLowerCase().contains("symlink externo")));
    }

    @Test
    void skipsLargeAndBinaryFiles() throws Exception {
        Files.writeString(tempDir.resolve("small.java"), "class Small {}");
        Files.write(tempDir.resolve("blob.bin"), new byte[]{0, 1, 2, 3});
        Files.write(tempDir.resolve("big.txt"), new byte[3_000_000]);

        WalkLimits limits = new WalkLimits(1_000_000L, 100, 10_000_000L, Set.of(), Set.of());
        SafeFileWalker.WalkResult result = SafeFileWalker.walk(tempDir, path -> true, limits);
        List<String> relatives = result.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
        assertEquals(List.of("small.java"), relatives);
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.contains("grande")));
    }

    @Test
    void deepTreeHasStableOrder() throws Exception {
        Files.createDirectories(tempDir.resolve("a/b"));
        Files.writeString(tempDir.resolve("a/b/z.java"), "class Z {}");
        Files.writeString(tempDir.resolve("a/b/a.java"), "class A {}");
        Files.writeString(tempDir.resolve("a/m.java"), "class M {}");

        SafeFileWalker.WalkResult first = SafeFileWalker.walk(tempDir, p -> true, WalkLimits.DEFAULT);
        SafeFileWalker.WalkResult second = SafeFileWalker.walk(tempDir, p -> true, WalkLimits.DEFAULT);
        List<String> order1 = first.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
        List<String> order2 = second.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
        assertEquals(order1, order2);
        assertEquals("a/b/a.java", order1.getFirst());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void inaccessibleFileIsControlled() throws Exception {
        Path denied = tempDir.resolve("secret-dir");
        Files.createDirectories(denied);
        Files.writeString(denied.resolve("hidden.java"), "class Hidden {}");
        Files.writeString(tempDir.resolve("visible.java"), "class Visible {}");
        try {
            Files.setPosixFilePermissions(denied, EnumSet.noneOf(PosixFilePermission.class));
            SafeFileWalker.WalkResult result = SafeFileWalker.walk(tempDir, p -> true, WalkLimits.DEFAULT);
            List<String> relatives = result.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
            assertTrue(relatives.contains("visible.java"));
            assertTrue(result.diagnostics().stream().anyMatch(d -> d.toLowerCase().contains("inacess")
                    || d.toLowerCase().contains("permissão")));
        } finally {
            Files.setPosixFilePermissions(denied, EnumSet.allOf(PosixFilePermission.class));
        }
    }

    @Test
    @Timeout(5)
    void symlinkCycleDoesNotRecurse() throws Exception {
        Files.writeString(tempDir.resolve("visible.txt"), "ok");
        Files.createSymbolicLink(tempDir.resolve("loop"), tempDir);

        SafeFileWalker.WalkResult result = SafeFileWalker.walk(tempDir, path -> true, WalkLimits.DEFAULT);
        List<String> relatives = result.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
        assertTrue(relatives.contains("visible.txt"));
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.toLowerCase().contains("já visitado")));
    }

    @Test
    void excludeSkipsRelativePathAndDirectoryName() throws Exception {
        Files.createDirectories(tempDir.resolve("keep"));
        Files.createDirectories(tempDir.resolve("secret-dir"));
        Files.writeString(tempDir.resolve("keep/App.java"), "class App {}");
        Files.writeString(tempDir.resolve("secret-dir/Hidden.java"), "class Hidden {}");
        Files.writeString(tempDir.resolve("skip-me.txt"), "nope");

        WalkLimits limits = WalkLimits.DEFAULT.withExclusions(Set.of("secret-dir", "skip-me.txt"));
        SafeFileWalker.WalkResult result = SafeFileWalker.walk(tempDir, path -> true, limits);
        List<String> relatives = result.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
        assertEquals(List.of("keep/App.java"), relatives);
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.contains("excluído")));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void unreadableFileIsReported() throws Exception {
        Path locked = tempDir.resolve("locked.txt");
        Files.writeString(locked, "conteudo");
        Files.writeString(tempDir.resolve("open.txt"), "ok");
        try {
            Files.setPosixFilePermissions(locked, EnumSet.noneOf(PosixFilePermission.class));
            SafeFileWalker.WalkResult result = SafeFileWalker.walk(tempDir, path -> true, WalkLimits.DEFAULT);
            List<String> relatives = result.files().stream().map(p -> SafeFileWalker.toRelative(tempDir, p)).toList();
            assertTrue(relatives.contains("open.txt"));
            assertFalse(relatives.contains("locked.txt"));
            assertTrue(result.diagnostics().stream().anyMatch(d -> d.toLowerCase().contains("leitura")
                    || d.toLowerCase().contains("inacess")
                    || d.toLowerCase().contains("permissão")));
        } finally {
            Files.setPosixFilePermissions(locked, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        }
    }

    @Test
    void pathTraversalRejectedOnRelativize() {
        Path root = tempDir.toAbsolutePath().normalize();
        Path outside = root.getParent().resolve("outside.txt");
        assertThrows(IllegalArgumentException.class, () -> SafeFileWalker.toRelative(root, outside));
    }
}

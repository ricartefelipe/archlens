package io.archlens.analyzer.core.walk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class SafeFileWalker {

    private static final Set<String> IGNORED_DIR_NAMES = Set.of(
            ".git", "target", "build", "node_modules", ".idea", ".gradle", "out", "dist"
    );

    private SafeFileWalker() {
    }

    public static List<Path> walkFiles(Path root, Predicate<Path> fileFilter) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        List<Path> files = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> isWithinRoot(normalizedRoot, path))
                    .filter(path -> !isUnderIgnoredDir(normalizedRoot, path))
                    .filter(fileFilter)
                    .forEach(files::add);
        }

        files.sort(Comparator.comparing(path -> normalizedRoot.relativize(path).toString()));
        return files;
    }

    public static boolean isWithinRoot(Path root, Path candidate) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }

    static boolean isUnderIgnoredDir(Path root, Path file) {
        Path relative = root.relativize(file.toAbsolutePath().normalize());
        for (Path segment : relative) {
            if (IGNORED_DIR_NAMES.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIgnoredDirName(String name) {
        return IGNORED_DIR_NAMES.contains(name);
    }
}

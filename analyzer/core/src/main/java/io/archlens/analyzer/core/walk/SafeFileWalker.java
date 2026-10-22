package io.archlens.analyzer.core.walk;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Caminhamento somente-leitura com raiz absoluta normalizada, sem seguir symlinks para fora,
 * ignores determinísticos e limites configuráveis.
 */
public final class SafeFileWalker {

    private static final Set<String> IGNORED_DIR_NAMES = Set.of(
            ".git", "target", "build", ".gradle", ".idea", ".vscode",
            "node_modules", "dist", "out"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "class", "jar", "war", "ear", "zip", "gz", "tgz", "bz2", "7z",
            "png", "jpg", "jpeg", "gif", "webp", "ico", "bmp", "pdf",
            "so", "dll", "dylib", "exe", "bin", "o", "a", "obj",
            "woff", "woff2", "ttf", "eot", "mp3", "mp4", "avi", "mov",
            "sqlite", "db", "dat"
    );

    private SafeFileWalker() {
    }

    public static List<Path> walkFiles(Path root, Predicate<Path> fileFilter) throws IOException {
        return walk(root, fileFilter, WalkLimits.DEFAULT).files();
    }

    public static WalkResult walk(Path root, Predicate<Path> fileFilter, WalkLimits limits) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(normalizedRoot)) {
            throw new IOException("Raiz não é diretório: " + normalizedRoot);
        }

        List<Path> files = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        long totalBytes = 0;
        Set<String> ignored = new HashSet<>(IGNORED_DIR_NAMES);
        ignored.addAll(limits.extraIgnoredDirNames());
        Set<Path> visitedRealDirs = new HashSet<>();

        totalBytes = walkRecursive(
                normalizedRoot,
                normalizedRoot,
                fileFilter,
                limits,
                ignored,
                files,
                diagnostics,
                totalBytes,
                visitedRealDirs
        );

        files.sort(Comparator.comparing(path -> toRelative(normalizedRoot, path)));
        return new WalkResult(normalizedRoot, List.copyOf(files), List.copyOf(diagnostics), totalBytes);
    }

    private static long walkRecursive(
            Path root,
            Path current,
            Predicate<Path> fileFilter,
            WalkLimits limits,
            Set<String> ignoredDirs,
            List<Path> files,
            List<String> diagnostics,
            long totalBytes,
            Set<Path> visitedRealDirs
    ) {
        Path realDir;
        try {
            realDir = current.toRealPath();
        } catch (IOException ex) {
            diagnostics.add("Permissão negada ou inacessível: " + safeRelative(root, current));
            return totalBytes;
        }
        if (!visitedRealDirs.add(realDir)) {
            diagnostics.add("Diretório já visitado ignorado: " + safeRelative(root, current));
            return totalBytes;
        }

        if (limits.maxFileCount() > 0 && files.size() >= limits.maxFileCount()) {
            diagnostics.add("Limite de quantidade de arquivos atingido");
            return totalBytes;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            List<Path> children = new ArrayList<>();
            for (Path child : stream) {
                children.add(child);
            }
            children.sort(Comparator.comparing(path -> toRelative(root, path)));

            for (Path child : children) {
                if (limits.maxFileCount() > 0 && files.size() >= limits.maxFileCount()) {
                    diagnostics.add("Limite de quantidade de arquivos atingido");
                    return totalBytes;
                }

                BasicFileAttributes attrs;
                try {
                    attrs = Files.readAttributes(child, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                } catch (IOException ex) {
                    diagnostics.add("Permissão negada ou inacessível: " + toRelative(root, child));
                    continue;
                }

                if (attrs.isSymbolicLink()) {
                    Path target;
                    try {
                        target = child.toRealPath();
                    } catch (IOException ex) {
                        diagnostics.add("Symlink quebrado ignorado: " + toRelative(root, child));
                        continue;
                    }
                    if (!isWithinRoot(root, target)) {
                        diagnostics.add("Symlink externo ignorado: " + toRelative(root, child));
                        continue;
                    }
                    if (isExcluded(root, child, limits)) {
                        diagnostics.add("Caminho excluído: " + toRelative(root, child));
                        continue;
                    }
                    if (Files.isDirectory(target)) {
                        if (isIgnoredDirName(target.getFileName().toString())
                                || isUnderIgnoredDir(root, child, ignoredDirs)) {
                            continue;
                        }
                        try {
                            if (visitedRealDirs.contains(target.toRealPath())) {
                                diagnostics.add("Diretório já visitado ignorado: " + toRelative(root, child));
                                continue;
                            }
                        } catch (IOException ex) {
                            diagnostics.add("Symlink quebrado ignorado: " + toRelative(root, child));
                            continue;
                        }
                        totalBytes = walkRecursive(root, target, fileFilter, limits, ignoredDirs, files, diagnostics, totalBytes, visitedRealDirs);
                    } else if (Files.isRegularFile(target)) {
                        totalBytes = maybeAddFile(root, child, target, attrs, fileFilter, limits, files, diagnostics, totalBytes);
                    }
                    continue;
                }

                if (attrs.isDirectory()) {
                    String name = child.getFileName().toString();
                    if (ignoredDirs.contains(name) || isExcluded(root, child, limits)) {
                        if (isExcluded(root, child, limits)) {
                            diagnostics.add("Caminho excluído: " + toRelative(root, child));
                        }
                        continue;
                    }
                    totalBytes = walkRecursive(root, child, fileFilter, limits, ignoredDirs, files, diagnostics, totalBytes, visitedRealDirs);
                    continue;
                }

                if (attrs.isRegularFile()) {
                    totalBytes = maybeAddFile(root, child, child, attrs, fileFilter, limits, files, diagnostics, totalBytes);
                }
            }
        } catch (IOException ex) {
            diagnostics.add("Permissão negada ou inacessível: " + toRelative(root, current));
        }
        return totalBytes;
    }

    private static long maybeAddFile(
            Path root,
            Path pathForRelative,
            Path readablePath,
            BasicFileAttributes attrs,
            Predicate<Path> fileFilter,
            WalkLimits limits,
            List<Path> files,
            List<String> diagnostics,
            long totalBytes
    ) {
        if (!isWithinRoot(root, readablePath)) {
            diagnostics.add("Path fora da raiz ignorado: " + toRelative(root, pathForRelative));
            return totalBytes;
        }
        if (isUnderIgnoredDir(root, pathForRelative, IGNORED_DIR_NAMES)) {
            return totalBytes;
        }
        if (isBinary(pathForRelative)) {
            return totalBytes;
        }
        if (isExcluded(root, pathForRelative, limits)) {
            diagnostics.add("Caminho excluído: " + toRelative(root, pathForRelative));
            return totalBytes;
        }
        long size = attrs.size();
        if (limits.maxFileBytes() > 0 && size > limits.maxFileBytes()) {
            diagnostics.add("Arquivo grande ignorado: " + toRelative(root, pathForRelative));
            return totalBytes;
        }
        if (limits.maxTotalBytes() > 0 && totalBytes + size > limits.maxTotalBytes()) {
            diagnostics.add("Limite total de bytes atingido");
            return totalBytes;
        }
        if (!fileFilter.test(pathForRelative)) {
            return totalBytes;
        }
        if (looksBinaryByContent(readablePath, size, root, pathForRelative, diagnostics)) {
            return totalBytes;
        }
        files.add(pathForRelative.toAbsolutePath().normalize());
        return totalBytes + size;
    }

    static boolean isExcluded(Path root, Path candidate, WalkLimits limits) {
        if (limits.excludedRelativePrefixes().isEmpty()) {
            return false;
        }
        String relative = toRelative(root, candidate);
        for (String raw : limits.excludedRelativePrefixes()) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String prefix = raw.replace('\\', '/').replaceAll("^\\./", "").replaceAll("^/+", "");
            if (prefix.isEmpty() || prefix.contains("..")) {
                continue;
            }
            while (prefix.endsWith("/")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            if (relative.equals(prefix) || relative.startsWith(prefix + "/")) {
                return true;
            }
            if (!prefix.contains("/")) {
                for (String segment : relative.split("/")) {
                    if (segment.equals(prefix)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String safeRelative(Path root, Path file) {
        try {
            String relative = toRelative(root, file);
            return relative.isEmpty() ? "." : relative;
        } catch (IllegalArgumentException ex) {
            return "(fora da raiz)";
        }
    }

    public static boolean isWithinRoot(Path root, Path candidate) {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path normalizedCandidate = candidate.toAbsolutePath().normalize();
        return normalizedCandidate.startsWith(normalizedRoot);
    }

    static boolean isUnderIgnoredDir(Path root, Path file) {
        return isUnderIgnoredDir(root, file, IGNORED_DIR_NAMES);
    }

    static boolean isUnderIgnoredDir(Path root, Path file, Set<String> ignoredDirs) {
        Path relative = root.relativize(file.toAbsolutePath().normalize());
        for (Path segment : relative) {
            if (ignoredDirs.contains(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIgnoredDirName(String name) {
        return IGNORED_DIR_NAMES.contains(name);
    }

    public static boolean isBinary(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return BINARY_EXTENSIONS.contains(fileName.substring(dot + 1));
    }

    public static String toRelative(Path root, Path file) {
        String relative = root.toAbsolutePath().normalize()
                .relativize(file.toAbsolutePath().normalize())
                .toString()
                .replace('\\', '/');
        if (relative.startsWith("../") || relative.equals("..")) {
            throw new IllegalArgumentException("Path traversal detectado");
        }
        return relative;
    }

    private static boolean looksBinaryByContent(
            Path path,
            long size,
            Path root,
            Path displayPath,
            List<String> diagnostics
    ) {
        if (size <= 0) {
            return false;
        }
        int sample = (int) Math.min(size, 512);
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = in.readNBytes(sample);
            for (byte b : buffer) {
                if (b == 0) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            diagnostics.add("Erro de leitura ignorado: " + safeRelative(root, displayPath));
            return true;
        }
    }

    public record WalkResult(Path root, List<Path> files, List<String> diagnostics, long totalBytes) {
    }
}

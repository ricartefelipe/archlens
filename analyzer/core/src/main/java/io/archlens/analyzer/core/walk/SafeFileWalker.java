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
            "node_modules", "dist", "out", "vendor", "third_party", "third-party", "bower_components"
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
        return walk(root, fileFilter, limits, null);
    }

    public static WalkResult walk(Path root, Predicate<Path> fileFilter, WalkLimits limits, WalkControl control) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot, LinkOption.NOFOLLOW_LINKS) && !Files.isDirectory(normalizedRoot)) {
            throw new IOException("Raiz não é diretório: " + normalizedRoot);
        }

        List<Path> files = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        long totalBytes = 0;
        Set<String> ignored = new HashSet<>(IGNORED_DIR_NAMES);
        ignored.addAll(limits.extraIgnoredDirNames());

        long deadlineNanos = limits.maxDurationMillis() > 0
                ? System.nanoTime() + limits.maxDurationMillis() * 1_000_000L
                : Long.MAX_VALUE;
        totalBytes = walkRecursive(
                normalizedRoot,
                normalizedRoot,
                fileFilter,
                limits,
                control,
                deadlineNanos,
                ignored,
                files,
                diagnostics,
                totalBytes
        );

        files.sort(Comparator.comparing(path -> toRelative(normalizedRoot, path)));
        return new WalkResult(normalizedRoot, List.copyOf(files), List.copyOf(diagnostics), totalBytes);
    }

    private static long walkRecursive(
            Path root,
            Path current,
            Predicate<Path> fileFilter,
            WalkLimits limits,
            WalkControl control,
            long deadlineNanos,
            Set<String> ignoredDirs,
            List<Path> files,
            List<String> diagnostics,
            long totalBytes
    ) {
        if (stopRequested(root, current, control, deadlineNanos, diagnostics)) {
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
                if (stopRequested(root, child, control, deadlineNanos, diagnostics)) {
                    return totalBytes;
                }
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
                    if (Files.isDirectory(target)) {
                        if (isIgnoredDirName(target.getFileName().toString())
                                || isUnderIgnoredDir(root, child, ignoredDirs)) {
                            diagnostics.add("Diretório ignorado: " + toRelative(root, child));
                            continue;
                        }
                        if (exceedsDepth(root, target, limits)) {
                            diagnostics.add("Profundidade máxima atingida: " + toRelative(root, child));
                            continue;
                        }
                        totalBytes = walkRecursive(root, target, fileFilter, limits, control, deadlineNanos, ignoredDirs, files, diagnostics, totalBytes);
                    } else if (Files.isRegularFile(target)) {
                        totalBytes = maybeAddFile(root, child, target, attrs, fileFilter, limits, ignoredDirs, files, diagnostics, totalBytes);
                    }
                    continue;
                }

                if (attrs.isDirectory()) {
                    String name = child.getFileName().toString();
                    if (ignoredDirs.contains(name)) {
                        diagnostics.add("Diretório ignorado: " + toRelative(root, child));
                        continue;
                    }
                    if (exceedsDepth(root, child, limits)) {
                        diagnostics.add("Profundidade máxima atingida: " + toRelative(root, child));
                        continue;
                    }
                    totalBytes = walkRecursive(root, child, fileFilter, limits, control, deadlineNanos, ignoredDirs, files, diagnostics, totalBytes);
                    continue;
                }

                if (attrs.isRegularFile()) {
                    totalBytes = maybeAddFile(root, child, child, attrs, fileFilter, limits, ignoredDirs, files, diagnostics, totalBytes);
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
            Set<String> ignoredDirs,
            List<Path> files,
            List<String> diagnostics,
            long totalBytes
    ) {
        if (!isWithinRoot(root, readablePath)) {
            diagnostics.add("Path fora da raiz ignorado: " + toRelative(root, pathForRelative));
            return totalBytes;
        }
        if (isUnderIgnoredDir(root, pathForRelative, ignoredDirs)) {
            return totalBytes;
        }
        if (isSensitive(pathForRelative)) {
            diagnostics.add("Arquivo sensível ignorado: " + toRelative(root, pathForRelative));
            return totalBytes;
        }
        if (isBinary(pathForRelative)) {
            diagnostics.add("Binário ignorado: " + toRelative(root, pathForRelative));
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
        if (looksBinaryByContent(readablePath, size)) {
            return totalBytes;
        }
        files.add(pathForRelative.toAbsolutePath().normalize());
        return totalBytes + size;
    }

    private static boolean stopRequested(
            Path root,
            Path current,
            WalkControl control,
            long deadlineNanos,
            List<String> diagnostics
    ) {
        if (control != null && control.isCancelled()) {
            if (diagnostics.stream().noneMatch(item -> item.contains("cancelad"))) {
                diagnostics.add("Caminhamento cancelado");
            }
            return true;
        }
        if (System.nanoTime() > deadlineNanos) {
            if (diagnostics.stream().noneMatch(item -> item.contains("tempo máximo"))) {
                diagnostics.add("Tempo máximo de caminhamento atingido");
            }
            return true;
        }
        return false;
    }

    private static boolean exceedsDepth(Path root, Path directory, WalkLimits limits) {
        if (limits.maxDepth() <= 0) {
            return false;
        }
        Path relative = root.toAbsolutePath().normalize().relativize(directory.toAbsolutePath().normalize());
        int depth = 0;
        for (Path segment : relative) {
            String name = segment.toString();
            if (!name.isBlank() && !".".equals(name)) {
                depth++;
            }
        }
        return depth > limits.maxDepth();
    }

    private static boolean isSensitive(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.equals(".env") || name.startsWith(".env.") || name.equals("id_rsa") || name.equals("id_dsa")
                || name.equals("id_ecdsa") || name.equals("credentials.json")) {
            return true;
        }
        return name.endsWith(".pem") || name.endsWith(".p12") || name.endsWith(".keystore")
                || name.endsWith(".jks") || name.endsWith(".key");
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

    private static boolean looksBinaryByContent(Path path, long size) {
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
            return true;
        }
    }

    public record WalkResult(Path root, List<Path> files, List<String> diagnostics, long totalBytes) {
    }
}

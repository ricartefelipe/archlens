package io.archlens.analyzer.core.walk;

import java.util.Objects;
import java.util.Set;

/**
 * Limites configuráveis do caminhamento seguro. Valores &lt;= 0 desabilitam o limite correspondente.
 */
public record WalkLimits(
        long maxFileBytes,
        int maxFileCount,
        long maxTotalBytes,
        int maxDepth,
        long maxDurationMillis,
        Set<String> extraIgnoredDirNames
) {
    public static final int DEFAULT_MAX_DEPTH = 32;
    public static final long DEFAULT_MAX_DURATION_MILLIS = 60_000L;

    public static final WalkLimits DEFAULT = new WalkLimits(
            2_000_000L,
            50_000,
            200_000_000L,
            DEFAULT_MAX_DEPTH,
            DEFAULT_MAX_DURATION_MILLIS,
            Set.of()
    );

    public WalkLimits(long maxFileBytes, int maxFileCount, long maxTotalBytes, Set<String> extraIgnoredDirNames) {
        this(maxFileBytes, maxFileCount, maxTotalBytes, DEFAULT_MAX_DEPTH, DEFAULT_MAX_DURATION_MILLIS, extraIgnoredDirNames);
    }

    public WalkLimits {
        Objects.requireNonNull(extraIgnoredDirNames, "extraIgnoredDirNames");
        extraIgnoredDirNames = Set.copyOf(extraIgnoredDirNames);
    }

    public WalkLimits withExtraIgnoredDirs(Set<String> dirs) {
        return new WalkLimits(maxFileBytes, maxFileCount, maxTotalBytes, maxDepth, maxDurationMillis, dirs);
    }
}

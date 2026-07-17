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
        Set<String> extraIgnoredDirNames
) {
    public static final WalkLimits DEFAULT = new WalkLimits(
            2_000_000L,
            50_000,
            200_000_000L,
            Set.of()
    );

    public WalkLimits {
        Objects.requireNonNull(extraIgnoredDirNames, "extraIgnoredDirNames");
        extraIgnoredDirNames = Set.copyOf(extraIgnoredDirNames);
    }

    public WalkLimits withExtraIgnoredDirs(Set<String> dirs) {
        return new WalkLimits(maxFileBytes, maxFileCount, maxTotalBytes, dirs);
    }
}

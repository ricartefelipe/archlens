package io.archlens.analyzer.core.walk;

import java.util.Objects;
import java.util.Set;

/**
 * Limites configuráveis do caminhamento seguro. Valores &lt;= 0 desabilitam o limite correspondente.
 * {@code excludedRelativePrefixes} aceita caminho relativo ({@code vendor/lib}) ou nome de diretório ({@code secrets}).
 */
public record WalkLimits(
        long maxFileBytes,
        int maxFileCount,
        long maxTotalBytes,
        Set<String> extraIgnoredDirNames,
        Set<String> excludedRelativePrefixes
) {
    public static final WalkLimits DEFAULT = new WalkLimits(
            2_000_000L,
            50_000,
            200_000_000L,
            Set.of(),
            Set.of()
    );

    public WalkLimits {
        Objects.requireNonNull(extraIgnoredDirNames, "extraIgnoredDirNames");
        Objects.requireNonNull(excludedRelativePrefixes, "excludedRelativePrefixes");
        extraIgnoredDirNames = Set.copyOf(extraIgnoredDirNames);
        excludedRelativePrefixes = Set.copyOf(excludedRelativePrefixes);
    }

    public WalkLimits withExtraIgnoredDirs(Set<String> dirs) {
        return new WalkLimits(maxFileBytes, maxFileCount, maxTotalBytes, dirs, excludedRelativePrefixes);
    }

    public WalkLimits withExclusions(Set<String> prefixes) {
        return new WalkLimits(maxFileBytes, maxFileCount, maxTotalBytes, extraIgnoredDirNames, prefixes);
    }
}

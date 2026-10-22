package io.archlens.analyzer.core.build;

import io.archlens.analyzer.core.model.Dependency;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leitura heurística de Gradle. Não avalia Groovy/Kotlin, não executa o build
 * e não resolve version catalogs.
 */
public final class GradleBuildParser {

    private static final Pattern CONFIGURATION = Pattern.compile(
            "(?i)\\b(testImplementation|testCompileOnly|testRuntimeOnly|compileOnly|runtimeOnly|annotationProcessor|testCompile|providedCompile|implementation|api|compile|runtime|provided)\\s*\\(?\\s*['\"]([^'\"]+)['\"]"
    );
    private static final Pattern PROJECT_DEP = Pattern.compile(
            "(?i)\\b(testImplementation|implementation|api|compile)\\s*\\(?\\s*project\\s*\\(\\s*['\"]([^'\"]+)['\"]"
    );
    private static final Pattern CATALOG_DEP = Pattern.compile(
            "(?i)\\b(testImplementation|implementation|api|compileOnly|runtimeOnly|compile)\\s*\\(?\\s*(libs\\.[A-Za-z0-9_.]+)"
    );
    private static final Pattern INCLUDE_LINE = Pattern.compile("(?i)\\binclude\\b([^\\n]*)");
    private static final Pattern QUOTED = Pattern.compile("['\"]([^'\"]+)['\"]");
    private static final Pattern SOURCE_COMPAT = Pattern.compile("(?i)(?:sourceCompatibility|targetCompatibility|jvmTarget)\\s*[=:]\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern JAVA_VERSION_ENUM = Pattern.compile("JavaVersion\\.VERSION_(\\d+)(?:_(\\d+))?");
    private static final Pattern TOOLCHAIN = Pattern.compile("JavaLanguageVersion\\.of\\(\\s*(\\d+)\\s*\\)");

    private GradleBuildParser() {
    }

    public record GradleParseResult(
            List<Dependency> dependencies,
            List<String> modules,
            Optional<String> javaVersionHint,
            String confidence,
            List<ParseLimitation> limitations
    ) {
    }

    public static GradleParseResult parse(String content, String sourcePath) {
        if (content == null || content.isBlank()) {
            return new GradleParseResult(List.of(), List.of(), Optional.empty(), "LOW",
                    List.of(new ParseLimitation("gradle-empty", "Arquivo Gradle vazio em " + sourcePath)));
        }
        String stripped = stripComments(content);
        List<ParseLimitation> limitations = new ArrayList<>();
        List<Dependency> dependencies = new ArrayList<>();
        Set<String> modules = new LinkedHashSet<>();

        Matcher projects = PROJECT_DEP.matcher(stripped);
        while (projects.find()) {
            String module = projects.group(2).replaceFirst("^:", "").replace(':', '/');
            if (!module.isBlank()) {
                modules.add(module);
            }
            limitations.add(new ParseLimitation(
                    "gradle-project-dependency",
                    "Dependência de projeto Gradle não expandida: " + projects.group(2)
            ));
        }

        Matcher catalogs = CATALOG_DEP.matcher(stripped);
        while (catalogs.find()) {
            limitations.add(new ParseLimitation(
                    "gradle-version-catalog",
                    "Version catalog não resolvido: " + catalogs.group(2)
            ));
        }

        Matcher deps = CONFIGURATION.matcher(stripped);
        while (deps.find()) {
            String scope = deps.group(1);
            String notation = deps.group(2).trim();
            if (notation.startsWith("libs.")) {
                limitations.add(new ParseLimitation(
                        "gradle-version-catalog",
                        "Version catalog não resolvido: " + notation
                ));
                continue;
            }
            if (notation.contains("project(")) {
                continue;
            }
            String[] parts = notation.split(":");
            if (parts.length >= 3 && !parts[0].isBlank() && !parts[1].isBlank()) {
                dependencies.add(new Dependency(parts[0] + ":" + parts[1] + ":" + parts[2], scope, sourcePath));
            } else if (parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
                dependencies.add(new Dependency(parts[0] + ":" + parts[1] + ":", scope, sourcePath));
                limitations.add(new ParseLimitation(
                        "gradle-version-missing",
                        "Versão Gradle ausente e não resolvida: " + notation
                ));
            } else {
                limitations.add(new ParseLimitation(
                        "gradle-notation-unsupported",
                        "Notação Gradle não suportada: " + notation
                ));
            }
        }

        if (sourcePath != null && (sourcePath.endsWith("settings.gradle") || sourcePath.endsWith("settings.gradle.kts"))) {
            Matcher includes = INCLUDE_LINE.matcher(stripped);
            while (includes.find()) {
                Matcher quoted = QUOTED.matcher(includes.group(1));
                while (quoted.find()) {
                    String module = quoted.group(1).replaceFirst("^:", "").replace(':', '/');
                    if (!module.isBlank()) {
                        modules.add(module);
                    }
                }
            }
        }

        if (stripped.contains("libs.versions.toml") || stripped.contains("versionCatalogs")) {
            limitations.add(new ParseLimitation(
                    "gradle-version-catalog",
                    "Version catalog declarado e não resolvido em " + sourcePath
            ));
        }

        String confidence = limitations.isEmpty() ? "MEDIUM" : "LOW";
        return new GradleParseResult(
                List.copyOf(dependencies),
                List.copyOf(modules),
                Optional.ofNullable(javaVersion(stripped)),
                confidence,
                List.copyOf(limitations)
        );
    }

    static String stripComments(String content) {
        String noBlock = content.replaceAll("(?s)/\\*.*?\\*/", " ");
        StringBuilder out = new StringBuilder();
        for (String line : noBlock.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                continue;
            }
            out.append(stripLineComment(line)).append('\n');
        }
        return out.toString();
    }

    private static String stripLineComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length() - 1; i++) {
            char current = line.charAt(i);
            if (current == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (current == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (current == '/' && line.charAt(i + 1) == '/' && !inSingle && !inDouble) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static String javaVersion(String content) {
        Matcher compat = SOURCE_COMPAT.matcher(content);
        if (compat.find()) {
            return MavenPomParser.normalizeJavaVersion(compat.group(1));
        }
        Matcher toolchain = TOOLCHAIN.matcher(content);
        if (toolchain.find()) {
            return toolchain.group(1);
        }
        Matcher enumerated = JAVA_VERSION_ENUM.matcher(content);
        if (enumerated.find()) {
            if (enumerated.group(2) != null && "1".equals(enumerated.group(1))) {
                return enumerated.group(2);
            }
            return enumerated.group(1);
        }
        return null;
    }
}

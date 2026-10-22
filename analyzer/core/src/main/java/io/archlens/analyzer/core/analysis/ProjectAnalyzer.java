package io.archlens.analyzer.core.analysis;

import io.archlens.analyzer.core.build.GradleBuildParser;
import io.archlens.analyzer.core.build.MavenPomParser;
import io.archlens.analyzer.core.detection.CycleDetector;
import io.archlens.analyzer.core.detection.SecretDetector;
import io.archlens.analyzer.core.detection.SignalDetector;
import io.archlens.analyzer.core.evidence.EvidenceFactory;
import io.archlens.analyzer.core.java.JavaSourceParser;
import io.archlens.analyzer.core.json.AnalysisJson;
import io.archlens.analyzer.core.model.AnalysisDocument;
import io.archlens.analyzer.core.model.Cycle;
import io.archlens.analyzer.core.model.Dependency;
import io.archlens.analyzer.core.model.EndpointInfo;
import io.archlens.analyzer.core.model.Evidence;
import io.archlens.analyzer.core.model.Hotspot;
import io.archlens.analyzer.core.model.Inventory;
import io.archlens.analyzer.core.model.MessagingInfo;
import io.archlens.analyzer.core.model.Metrics;
import io.archlens.analyzer.core.model.ModuleInfo;
import io.archlens.analyzer.core.model.PackageInfo;
import io.archlens.analyzer.core.model.PersistenceInfo;
import io.archlens.analyzer.core.model.ProjectInfo;
import io.archlens.analyzer.core.model.Secret;
import io.archlens.analyzer.core.model.Signal;
import io.archlens.analyzer.core.model.TypeInfo;
import io.archlens.analyzer.core.walk.SafeFileWalker;
import io.archlens.analyzer.core.walk.WalkLimits;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ProjectAnalyzer {

    public static final String SCHEMA_VERSION = "1.0.0";
    public static final String ANALYZER_VERSION = "0.1.0-SNAPSHOT";
    private static final int HOTSPOT_LINE_THRESHOLD = 80;
    private static final int MAX_TEXT_CHARS = 1_000_000;

    private ProjectAnalyzer() {
    }

    public static AnalysisDocument analyze(Path projectRoot) throws IOException {
        return analyze(projectRoot, WalkLimits.DEFAULT);
    }

    public static AnalysisDocument analyze(Path projectRoot, WalkLimits limits) throws IOException {
        long started = System.nanoTime();
        Path root = projectRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            throw new IOException("Project path is not a directory: " + root);
        }

        SafeFileWalker.WalkResult walk = SafeFileWalker.walk(root, path -> true, limits);
        List<Path> allFiles = walk.files();
        List<Path> javaFiles = allFiles.stream()
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .toList();

        Map<String, Evidence> evidenceById = new TreeMap<>();
        List<Dependency> dependencies = new ArrayList<>();
        Set<String> buildSystems = new TreeSet<>();
        String javaVersionHint = null;
        Map<String, ModuleInfo> modulesByPath = new TreeMap<>();
        List<SignalDetector.DetectedSignal> detectedSignals = new ArrayList<>();

        for (Path file : allFiles) {
            String relative = SafeFileWalker.toRelative(root, file);
            String name = file.getFileName().toString();
            detectedSignals.addAll(SignalDetector.detectFromPath(relative));

            if ("pom.xml".equals(name)) {
                buildSystems.add("MAVEN");
                String content = readText(file);
                MavenPomParser.PomParseResult pom = MavenPomParser.parse(content, relative);
                dependencies.addAll(pom.dependencies());
                javaVersionHint = firstNonNull(javaVersionHint, pom.javaVersionHint().orElse(null));
                modulesByPath.putIfAbsent(modulePath(relative), new ModuleInfo(moduleName(relative), modulePath(relative), "MAVEN"));
                for (String module : pom.modules()) {
                    String moduleRel = modulePath(relative).equals(".") ? module : modulePath(relative) + "/" + module;
                    modulesByPath.putIfAbsent(moduleRel, new ModuleInfo(module, moduleRel, "MAVEN"));
                }
                addManifestEvidence(evidenceById, relative, "pom.xml");
            } else if (isGradleBuild(name)) {
                buildSystems.add("GRADLE");
                String content = readText(file);
                GradleBuildParser.GradleParseResult gradle = GradleBuildParser.parse(content, relative);
                dependencies.addAll(gradle.dependencies());
                javaVersionHint = firstNonNull(javaVersionHint, gradle.javaVersionHint().orElse(null));
                if (!name.startsWith("settings.")) {
                    modulesByPath.putIfAbsent(modulePath(relative), new ModuleInfo(moduleName(relative), modulePath(relative), "GRADLE"));
                }
                for (String module : gradle.modules()) {
                    modulesByPath.putIfAbsent(module, new ModuleInfo(module, module, "GRADLE"));
                }
                addManifestEvidence(evidenceById, relative, name);
            } else if (isConfigFile(name)) {
                String content = readText(file);
                detectedSignals.addAll(SignalDetector.detectFromConfigContent(relative, content));
            }
        }

        if (buildSystems.isEmpty()) {
            buildSystems.add("UNKNOWN");
            modulesByPath.putIfAbsent(".", new ModuleInfo(root.getFileName().toString(), ".", "UNKNOWN"));
        }

        dependencies.sort(Comparator.comparing(Dependency::coordinates).thenComparing(Dependency::sourcePath));

        Map<String, PackageStats> packageStats = new TreeMap<>();
        List<TypeInfo> types = new ArrayList<>();
        List<EndpointInfo> endpoints = new ArrayList<>();
        List<PersistenceInfo> persistence = new ArrayList<>();
        List<MessagingInfo> messaging = new ArrayList<>();
        List<Secret> secrets = new ArrayList<>();
        List<Hotspot> hotspots = new ArrayList<>();
        Map<String, Set<String>> packageImports = new HashMap<>();
        Map<String, Set<String>> packageToFiles = new HashMap<>();
        detectedSignals.addAll(SignalDetector.detectFromDependencies(dependencies));

        int totalLines = 0;
        int testFileCount = 0;

        for (Path file : allFiles) {
            String relative = SafeFileWalker.toRelative(root, file);
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (isSecretCandidate(name)) {
                String content = readText(file);
                for (SecretDetector.SecretFinding finding : SecretDetector.detect(relative, content)) {
                    secrets.add(finding.secret());
                    evidenceById.put(finding.evidence().id(), finding.evidence());
                }
            }
        }

        for (Path javaFile : javaFiles) {
            String relative = SafeFileWalker.toRelative(root, javaFile);
            String content = readText(javaFile);
            int lines = JavaSourceParser.lineCount(content);
            totalLines += lines;

            if (isTestFile(relative)) {
                testFileCount++;
            }

            Optional<String> packageName = JavaSourceParser.packageName(content);
            String pkg = packageName.orElse("");
            packageStats.computeIfAbsent(pkg, key -> new PackageStats()).add(lines);
            packageToFiles.computeIfAbsent(pkg, key -> new TreeSet<>()).add(relative);

            List<String> imports = JavaSourceParser.imports(content);
            List<String> annotations = JavaSourceParser.annotations(content);
            detectedSignals.addAll(SignalDetector.detectFromSource(relative, content, imports, annotations));

            Optional<JavaSourceParser.TypeDeclaration> typeDecl = JavaSourceParser.primaryType(content);
            if (typeDecl.isPresent()) {
                String fqn = pkg.isEmpty() ? typeDecl.get().simpleName() : pkg + "." + typeDecl.get().simpleName();
                types.add(new TypeInfo(
                        fqn,
                        typeDecl.get().kind(),
                        pkg,
                        relative,
                        lines,
                        sortedCopy(annotations)
                ));
            }

            registerImports(pkg, imports, packageImports, root);

            for (SecretDetector.SecretFinding finding : SecretDetector.detect(relative, content)) {
                secrets.add(finding.secret());
                evidenceById.put(finding.evidence().id(), finding.evidence());
            }

            if (lines >= HOTSPOT_LINE_THRESHOLD) {
                String evidenceId = EvidenceFactory.evidenceId("hotspot", relative, null);
                evidenceById.putIfAbsent(evidenceId, EvidenceFactory.fileEvidence(evidenceId, relative));
                hotspots.add(new Hotspot(relative, lines, "Large file", evidenceId));
            }

            collectInventoryFromSource(relative, content, imports, annotations, endpoints, persistence, messaging, evidenceById);
        }

        types.sort(Comparator.comparing(TypeInfo::fqn));
        endpoints.sort(Comparator.comparing(EndpointInfo::sourcePath).thenComparing(EndpointInfo::pathOrName));
        persistence.sort(Comparator.comparing(PersistenceInfo::sourcePath).thenComparing(PersistenceInfo::name));
        messaging.sort(Comparator.comparing(MessagingInfo::sourcePath).thenComparing(MessagingInfo::name));
        secrets.sort(Comparator.comparing(Secret::path).thenComparingInt(Secret::line).thenComparing(Secret::type));
        hotspots.sort(Comparator.comparing(Hotspot::path));

        List<PackageInfo> packages = packageStats.entrySet().stream()
                .map(entry -> new PackageInfo(
                        entry.getKey(),
                        packageToFiles.getOrDefault(entry.getKey(), Set.of()).size(),
                        entry.getValue().lineCount()
                ))
                .sorted(Comparator.comparing(PackageInfo::name))
                .toList();

        List<ModuleInfo> modules = new ArrayList<>(modulesByPath.values());
        modules.sort(Comparator.comparing(ModuleInfo::path));

        List<Signal> signals = mergeSignals(detectedSignals, evidenceById);
        addBuildSignals(buildSystems, evidenceById, signals);
        addInsufficientTestsSignal(javaFiles.size(), testFileCount, evidenceById, signals);
        signals.sort(Comparator.comparing(Signal::id));
        signals.addAll(walkDiagnostics(walk.diagnostics(), evidenceById));
        signals.sort(Comparator.comparing(Signal::id));

        List<Cycle> cycles = detectCycles(packageImports, packageToFiles, evidenceById);

        List<Evidence> evidences = new ArrayList<>(evidenceById.values());
        evidences.sort(Comparator.comparing(Evidence::id));

        Metrics metrics = new Metrics(
                allFiles.size(),
                javaFiles.size(),
                totalLines,
                types.size(),
                testFileCount,
                dependencies.size()
        );

        ProjectInfo project = new ProjectInfo(
                root.getFileName().toString(),
                List.copyOf(buildSystems),
                List.of("Java"),
                javaVersionHint
        );

        Inventory inventory = new Inventory(
                modules,
                packages,
                types,
                endpoints,
                persistence,
                messaging
        );

        long durationMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);

        AnalysisDocument draft = new AnalysisDocument(
                SCHEMA_VERSION,
                ANALYZER_VERSION,
                Instant.now().toString(),
                durationMs,
                "sha256:" + "0".repeat(64),
                project,
                inventory,
                signals,
                dependencies,
                hotspots,
                cycles,
                secrets,
                evidences,
                metrics
        );

        return AnalysisJson.withHash(draft);
    }

    private static boolean isSecretCandidate(String name) {
        return name.endsWith(".properties")
                || name.endsWith(".yml")
                || name.endsWith(".yaml")
                || name.endsWith(".xml")
                || name.endsWith(".env")
                || name.endsWith(".json")
                || name.endsWith(".sh")
                || name.endsWith(".bash")
                || name.endsWith(".conf")
                || name.endsWith(".cfg");
    }

    private static List<Signal> walkDiagnostics(List<String> diagnostics, Map<String, Evidence> evidenceById) {
        List<Signal> signals = new ArrayList<>();
        int index = 0;
        for (String diagnostic : diagnostics) {
            String safe = diagnostic == null ? "" : diagnostic.replace('\n', ' ').trim();
            if (safe.isEmpty()) {
                continue;
            }
            if (safe.length() > 180) {
                safe = safe.substring(0, 180);
            }
            String id = "walk.diagnostic." + index++;
            String evidenceId = "walk:" + id;
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.patternEvidence(evidenceId, ".", "walk", id));
            signals.add(new Signal(id, "OTHER", safe, "HIGH", List.of(evidenceId)));
        }
        return signals;
    }

    private static String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length > MAX_TEXT_CHARS) {
            return new String(bytes, 0, MAX_TEXT_CHARS, StandardCharsets.UTF_8);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isGradleBuild(String name) {
        return "build.gradle".equals(name)
                || "build.gradle.kts".equals(name)
                || "settings.gradle".equals(name)
                || "settings.gradle.kts".equals(name);
    }

    private static boolean isConfigFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".properties")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.equals("web.xml")
                || lower.equals("persistence.xml")
                || lower.equals("beans.xml");
    }

    private static void addBuildSignals(Set<String> buildSystems, Map<String, Evidence> evidenceById, List<Signal> signals) {
        if (buildSystems.contains("MAVEN")) {
            String evidenceId = "build:maven";
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.manifestEvidence(evidenceId, "pom.xml", "maven"));
            signals.add(new Signal("build.maven", "BUILD", "Maven", "HIGH", List.of(evidenceId)));
        }
        if (buildSystems.contains("GRADLE")) {
            String evidenceId = "build:gradle";
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.manifestEvidence(evidenceId, "build.gradle", "gradle"));
            signals.add(new Signal("build.gradle", "BUILD", "Gradle", "HIGH", List.of(evidenceId)));
        }
    }

    private static void addInsufficientTestsSignal(
            int javaFileCount,
            int testFileCount,
            Map<String, Evidence> evidenceById,
            List<Signal> signals
    ) {
        if (javaFileCount > 0 && testFileCount * 5 < javaFileCount) {
            String evidenceId = "quality:insufficient-tests";
            evidenceById.putIfAbsent(
                    evidenceId,
                    EvidenceFactory.patternEvidence(evidenceId, ".", "test-ratio", "tests-count")
            );
            signals.add(new Signal(
                    "quality.insufficient-tests",
                    "QUALITY",
                    "Insufficient tests",
                    "MEDIUM",
                    List.of(evidenceId)
            ));
        }
    }

    private static List<Signal> mergeSignals(
            List<SignalDetector.DetectedSignal> detected,
            Map<String, Evidence> evidenceById
    ) {
        Map<String, SignalAccumulator> byId = new LinkedHashMap<>();
        for (SignalDetector.DetectedSignal item : detected) {
            String evidenceId = item.evidenceKey();
            evidenceById.putIfAbsent(
                    evidenceId,
                    EvidenceFactory.patternEvidence(
                            evidenceId,
                            evidencePath(evidenceId),
                            item.detector(),
                            item.id()
                    )
            );
            byId.computeIfAbsent(item.id(), key -> new SignalAccumulator(item))
                    .evidenceIds.add(evidenceId);
        }

        List<Signal> signals = new ArrayList<>();
        for (SignalAccumulator acc : byId.values()) {
            List<String> evidenceIds = acc.evidenceIds.stream().sorted().distinct().toList();
            signals.add(new Signal(acc.id, acc.category, acc.name, acc.confidence, evidenceIds));
        }
        return signals;
    }

    private static String evidencePath(String evidenceKey) {
        if (evidenceKey.startsWith("src:")) {
            String remainder = evidenceKey.substring(4);
            int colon = remainder.lastIndexOf(':');
            return colon > 0 ? remainder.substring(0, colon) : remainder;
        }
        if (evidenceKey.startsWith("dep:")) {
            return "pom.xml";
        }
        return ".";
    }

    private static List<Cycle> detectCycles(
            Map<String, Set<String>> packageImports,
            Map<String, Set<String>> packageToFiles,
            Map<String, Evidence> evidenceById
    ) {
        Map<String, Set<String>> adjacency = CycleDetector.buildAdjacency(packageImports);
        List<List<String>> rawCycles = CycleDetector.findPackageCycles(adjacency);
        List<Cycle> cycles = new ArrayList<>();

        for (List<String> cyclePackages : rawCycles) {
            List<String> evidenceIds = new ArrayList<>();
            for (String pkg : cyclePackages) {
                for (String file : packageToFiles.getOrDefault(pkg, Set.of())) {
                    String evidenceId = EvidenceFactory.evidenceId("cycle", file, pkg);
                    evidenceById.putIfAbsent(evidenceId, EvidenceFactory.fileEvidence(evidenceId, file));
                    evidenceIds.add(evidenceId);
                }
            }
            evidenceIds = evidenceIds.stream().sorted().distinct().toList();
            if (!evidenceIds.isEmpty()) {
                cycles.add(new Cycle(List.copyOf(cyclePackages), evidenceIds));
            }
        }

        cycles.sort(Comparator.comparing(cycle -> String.join("->", cycle.packages())));
        return cycles;
    }

    private static void collectInventoryFromSource(
            String relative,
            String content,
            List<String> imports,
            List<String> annotations,
            List<EndpointInfo> endpoints,
            List<PersistenceInfo> persistence,
            List<MessagingInfo> messaging,
            Map<String, Evidence> evidenceById
    ) {
        if (content.contains("@Path") || imports.stream().anyMatch(i -> i.startsWith("javax.ws.rs") || i.startsWith("jakarta.ws.rs"))) {
            String path = extractJaxRsPath(content);
            String evidenceId = EvidenceFactory.evidenceId("endpoint", relative, "jaxrs");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "jaxrs"));
            endpoints.add(new EndpointInfo("JAX_RS", path, relative, evidenceId));
        }

        if (imports.stream().anyMatch(i -> i.startsWith("javax.servlet") || i.startsWith("jakarta.servlet"))
                || content.toLowerCase(Locale.ROOT).contains("extends httpservlet")) {
            String evidenceId = EvidenceFactory.evidenceId("endpoint", relative, "servlet");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "servlet"));
            endpoints.add(new EndpointInfo("SERVLET", extractSimpleName(content, "Servlet"), relative, evidenceId));
        }

        if (annotations.contains("Entity") || imports.stream().anyMatch(i -> i.contains("persistence"))) {
            String entityName = extractSimpleName(content, "Entity");
            String evidenceId = EvidenceFactory.evidenceId("persistence", relative, "jpa");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "jpa"));
            persistence.add(new PersistenceInfo("JPA", entityName, relative, evidenceId));
        }

        if (imports.stream().anyMatch(i -> i.startsWith("java.sql") || i.startsWith("javax.sql"))) {
            String evidenceId = EvidenceFactory.evidenceId("persistence", relative, "jdbc");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "jdbc"));
            persistence.add(new PersistenceInfo("JDBC", extractSimpleName(content, "Jdbc"), relative, evidenceId));
        }

        if (content.contains("@WebService") || imports.stream().anyMatch(i -> i.startsWith("javax.jws") || i.startsWith("jakarta.jws"))) {
            String serviceName = extractSimpleName(content, "WebService");
            String evidenceId = EvidenceFactory.evidenceId("endpoint", relative, "soap");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "soap"));
            endpoints.add(new EndpointInfo("SOAP", serviceName, relative, evidenceId));
        }

        if (imports.stream().anyMatch(i -> i.startsWith("javax.jms") || i.startsWith("jakarta.jms"))) {
            String name = extractSimpleName(content, "JMS");
            String evidenceId = EvidenceFactory.evidenceId("messaging", relative, "jms");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "jms"));
            messaging.add(new MessagingInfo("JMS", name, relative, evidenceId));
        }

        if (imports.stream().anyMatch(i -> i.contains("kafka"))) {
            String name = extractSimpleName(content, "Kafka");
            String evidenceId = EvidenceFactory.evidenceId("messaging", relative, "kafka");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "kafka"));
            messaging.add(new MessagingInfo("KAFKA", name, relative, evidenceId));
        }

        if (imports.stream().anyMatch(i -> i.contains("rabbitmq") || i.contains("springframework.amqp"))) {
            String name = extractSimpleName(content, "Rabbit");
            String evidenceId = EvidenceFactory.evidenceId("messaging", relative, "rabbit");
            evidenceById.putIfAbsent(evidenceId, EvidenceFactory.lineEvidence(evidenceId, relative, 1, "rabbit"));
            messaging.add(new MessagingInfo("RABBITMQ", name, relative, evidenceId));
        }
    }

    private static void registerImports(
            String sourcePackage,
            List<String> imports,
            Map<String, Set<String>> packageImports,
            Path root
    ) {
        if (sourcePackage == null || sourcePackage.isEmpty()) {
            return;
        }
        Set<String> targets = packageImports.computeIfAbsent(sourcePackage, key -> new TreeSet<>());
        for (String imp : imports) {
            if (imp.endsWith(".*")) {
                continue;
            }
            int lastDot = imp.lastIndexOf('.');
            if (lastDot <= 0) {
                continue;
            }
            String targetPackage = imp.substring(0, lastDot);
            if (isProjectPackage(targetPackage, root)) {
                targets.add(targetPackage);
            }
        }
    }

    private static boolean isProjectPackage(String packageName, Path root) {
        Path packagePath = root.resolve("src").resolve("main").resolve("java").resolve(packageName.replace('.', '/'));
        return Files.isDirectory(packagePath);
    }

    private static String extractJaxRsPath(String content) {
        int idx = content.indexOf("@Path");
        if (idx >= 0) {
            int quote = content.indexOf('"', idx);
            if (quote >= 0) {
                int end = content.indexOf('"', quote + 1);
                if (end > quote) {
                    return content.substring(quote + 1, end);
                }
            }
        }
        return "/";
    }

    private static String extractSimpleName(String content, String fallback) {
        Optional<JavaSourceParser.TypeDeclaration> type = JavaSourceParser.primaryType(content);
        return type.map(JavaSourceParser.TypeDeclaration::simpleName).orElse(fallback);
    }

    private static void addManifestEvidence(Map<String, Evidence> evidenceById, String relative, String locator) {
        String evidenceId = EvidenceFactory.evidenceId("manifest", relative, locator);
        evidenceById.putIfAbsent(evidenceId, EvidenceFactory.manifestEvidence(evidenceId, relative, locator));
    }

    private static boolean isTestFile(String relativePath) {
        String normalized = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.contains("/test/") || normalized.endsWith("test.java") || normalized.contains("tests/");
    }

    private static String moduleName(String pomRelative) {
        if ("pom.xml".equals(pomRelative) || "build.gradle".equals(pomRelative) || "build.gradle.kts".equals(pomRelative)) {
            return ".";
        }
        Path parent = Path.of(pomRelative).getParent();
        return parent == null ? "." : parent.toString().replace('\\', '/');
    }

    private static String modulePath(String buildFileRelative) {
        Path parent = Path.of(buildFileRelative).getParent();
        return parent == null ? "." : parent.toString().replace('\\', '/');
    }

    private static String firstNonNull(String current, String candidate) {
        return current != null ? current : candidate;
    }

    private static List<String> sortedCopy(List<String> values) {
        List<String> copy = new ArrayList<>(values);
        copy.sort(String::compareTo);
        return copy;
    }

    private static final class PackageStats {
        private int lineCount;

        void add(int lines) {
            lineCount += lines;
        }

        int lineCount() {
            return lineCount;
        }
    }

    private static final class SignalAccumulator {
        private final String id;
        private final String category;
        private final String name;
        private final String confidence;
        private final Set<String> evidenceIds = new TreeSet<>();

        private SignalAccumulator(SignalDetector.DetectedSignal signal) {
            this.id = signal.id();
            this.category = signal.category();
            this.name = signal.name();
            this.confidence = signal.confidence();
        }
    }
}

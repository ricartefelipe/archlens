package io.archlens.analyzer.cli.command;

import io.archlens.analyzer.core.analysis.ProjectAnalyzer;
import io.archlens.analyzer.core.json.AnalysisJson;
import io.archlens.analyzer.core.model.AnalysisDocument;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "scan", description = "Analyze a Java project and write analysis.json")
public final class ScanCommand implements Callable<Integer> {

    @Option(names = {"--path"}, required = true, description = "Project root directory")
    private Path path;

    @Option(names = {"--out"}, required = true, description = "Output JSON file")
    private Path out;

    @Override
    public Integer call() {
        try {
            if (!Files.isDirectory(path)) {
                System.err.println("Path is not a directory: " + path);
                return 1;
            }
            AnalysisDocument document = ProjectAnalyzer.analyze(path);
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(out, AnalysisJson.writePretty(document));
            System.out.println("Analysis written to " + out.toAbsolutePath().normalize());
            return 0;
        } catch (Exception e) {
            System.err.println("Scan failed: " + e.getMessage());
            return 1;
        }
    }
}

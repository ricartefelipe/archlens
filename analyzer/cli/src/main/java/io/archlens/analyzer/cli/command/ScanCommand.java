package io.archlens.analyzer.cli.command;

import io.archlens.analyzer.core.analysis.ProjectAnalyzer;
import io.archlens.analyzer.core.cli.ExitCodes;
import io.archlens.analyzer.core.json.AnalysisJson;
import io.archlens.analyzer.core.model.AnalysisDocument;
import io.archlens.analyzer.core.walk.WalkLimits;
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
            if (path == null || !Files.isDirectory(path)) {
                System.err.println("Entrada inválida: caminho não é um diretório");
                return ExitCodes.INVALID_INPUT;
            }
            ProjectAnalyzer.ScanOutcome outcome = ProjectAnalyzer.analyze(path, WalkLimits.DEFAULT, null);
            AnalysisDocument document = outcome.document();
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(out, AnalysisJson.writePretty(document));
            for (String diagnostic : outcome.diagnostics()) {
                System.err.println("ignorado: " + diagnostic);
            }
            System.out.println("Analysis written to " + out.toAbsolutePath().normalize());
            return ExitCodes.SUCCESS;
        } catch (Exception e) {
            System.err.println("Erro interno na análise");
            return ExitCodes.INTERNAL_ERROR;
        }
    }
}

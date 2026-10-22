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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "scan", description = "Analyze a Java project and write analysis.json")
public final class ScanCommand implements Callable<Integer> {

    @Option(names = {"--path"}, required = true, description = "Project root directory")
    private Path path;

    @Option(names = {"--out"}, required = true, description = "Output JSON file")
    private Path out;

    @Option(names = {"--exclude"}, description = "Caminho relativo ou nome de diretório a ignorar. Repetível.")
    private List<String> exclude = new ArrayList<>();

    @Override
    public Integer call() {
        try {
            if (path == null || !Files.isDirectory(path)) {
                System.err.println("Entrada inválida: caminho não é um diretório");
                return ExitCodes.INVALID_INPUT;
            }
            for (String item : exclude) {
                if (item == null || item.isBlank() || item.contains("..") || item.startsWith("/") || item.contains(":")) {
                    System.err.println("Entrada inválida: exclusão rejeitada");
                    return ExitCodes.INVALID_INPUT;
                }
            }
            WalkLimits limits = WalkLimits.DEFAULT.withExclusions(Set.copyOf(exclude));
            AnalysisDocument document = ProjectAnalyzer.analyze(path, limits);
            Path parent = out.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(out, AnalysisJson.writePretty(document));
            System.out.println("Analysis written to " + out.toAbsolutePath().normalize());
            return ExitCodes.SUCCESS;
        } catch (Exception e) {
            System.err.println("Erro interno na análise");
            return ExitCodes.INTERNAL_ERROR;
        }
    }
}

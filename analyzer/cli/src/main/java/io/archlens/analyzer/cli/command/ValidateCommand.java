package io.archlens.analyzer.cli.command;

import io.archlens.analyzer.core.cli.ExitCodes;
import io.archlens.analyzer.core.schema.SchemaValidator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "validate", description = "Validate an analysis.json file against the schema")
public final class ValidateCommand implements Callable<Integer> {

    @Option(names = {"--file"}, required = true, description = "analysis.json file")
    private Path file;

    @Option(names = {"--schema"}, description = "Schema path (defaults to contracts/analysis.schema.json)")
    private Path schema;

    @Override
    public Integer call() {
        try {
            if (file == null || !Files.isRegularFile(file)) {
                System.err.println("Entrada inválida: arquivo de análise não encontrado");
                return ExitCodes.INVALID_INPUT;
            }
            Path schemaPath = resolveSchema();
            if (!Files.isRegularFile(schemaPath)) {
                System.err.println("Entrada inválida: schema não encontrado");
                return ExitCodes.INVALID_INPUT;
            }
            SchemaValidator.ValidationResult result = SchemaValidator.validate(schemaPath, file);
            if (result.success()) {
                System.out.println("Valid: " + file.toAbsolutePath().normalize());
                return ExitCodes.SUCCESS;
            }
            System.err.println("Validação falhou: JSON incompatível com o schema");
            return ExitCodes.VALIDATION_FAILED;
        } catch (Exception e) {
            System.err.println("Erro interno na validação");
            return ExitCodes.INTERNAL_ERROR;
        }
    }

    private Path resolveSchema() {
        if (schema != null) {
            return schema;
        }
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("contracts/analysis.schema.json");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return Path.of("contracts/analysis.schema.json");
    }
}

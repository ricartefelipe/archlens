package io.archlens.analyzer.cli.command;

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
            if (!Files.isRegularFile(file)) {
                System.err.println("File not found: " + file);
                return 1;
            }
            Path schemaPath = resolveSchema();
            if (!Files.isRegularFile(schemaPath)) {
                System.err.println("Schema not found: " + schemaPath);
                return 1;
            }
            SchemaValidator.ValidationResult result = SchemaValidator.validate(schemaPath, file);
            if (result.success()) {
                System.out.println("Valid: " + file.toAbsolutePath().normalize());
                return 0;
            }
            System.err.println("Validation failed: " + result.message());
            return 1;
        } catch (Exception e) {
            System.err.println("Validation error: " + e.getMessage());
            return 1;
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

package io.archlens.analyzer.core.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public final class SchemaValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SchemaValidator() {
    }

    public static ValidationResult validate(Path schemaPath, Path analysisFile) throws IOException {
        String json = Files.readString(analysisFile);
        return validateJson(schemaPath, json);
    }

    public static ValidationResult validateJson(Path schemaPath, String analysisJson) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(Files.readString(schemaPath));
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaNode);
        JsonNode dataNode = MAPPER.readTree(analysisJson);
        Set<ValidationMessage> errors = schema.validate(dataNode);
        if (errors.isEmpty()) {
            return ValidationResult.ok();
        }
        String message = errors.stream()
                .map(ValidationMessage::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return ValidationResult.invalid(message);
    }

    public static ValidationResult validateFromClasspath(String resourcePath, String analysisJson) throws IOException {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        try (InputStream in = SchemaValidator.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Schema resource not found: " + resourcePath);
            }
            JsonNode schemaNode = MAPPER.readTree(in);
            JsonSchema schema = factory.getSchema(schemaNode);
            JsonNode dataNode = MAPPER.readTree(analysisJson);
            Set<ValidationMessage> errors = schema.validate(dataNode);
            if (errors.isEmpty()) {
                return ValidationResult.ok();
            }
            String message = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .sorted()
                    .collect(Collectors.joining("; "));
            return ValidationResult.invalid(message);
        }
    }

    public record ValidationResult(boolean success, String message) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }
}

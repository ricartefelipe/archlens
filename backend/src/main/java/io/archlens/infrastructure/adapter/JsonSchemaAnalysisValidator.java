package io.archlens.infrastructure.adapter;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.archlens.application.port.AnalysisSchemaValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class JsonSchemaAnalysisValidator implements AnalysisSchemaValidator {
    private final JsonSchema schema;

    public JsonSchemaAnalysisValidator() {
        try (InputStream in = getClass().getResourceAsStream("/analysis.schema.json")) {
            if (in == null) {
                throw new IllegalStateException("analysis.schema.json not found on classpath");
            }
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
            this.schema = factory.getSchema(in);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load analysis schema", e);
        }
    }

    @Override
    public List<String> validate(String json) {
        List<String> errors = new ArrayList<>();
        Set<ValidationMessage> messages = schema.validate(json, InputFormat.JSON);
        for (ValidationMessage msg : messages) {
            errors.add(msg.getMessage());
        }
        return errors;
    }
}

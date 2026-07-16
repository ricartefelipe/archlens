package io.archlens.application.port;

import java.util.List;

public interface AnalysisSchemaValidator {
    List<String> validate(String json);
}

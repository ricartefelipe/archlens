package io.archlens.analyzer.core.cli;

/**
 * Códigos de saída do CLI ArchLens.
 */
public final class ExitCodes {

    public static final int SUCCESS = 0;
    public static final int INTERNAL_ERROR = 1;
    public static final int INVALID_INPUT = 2;
    public static final int VALIDATION_FAILED = 3;

    private ExitCodes() {
    }
}

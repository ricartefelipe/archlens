package io.archlens.analyzer.core.build;

/**
 * Limitação explícita de um parser estático. Não é uma falha comprovada do projeto analisado.
 */
public record ParseLimitation(String code, String message) {
}

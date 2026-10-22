package io.archlens.domain;

/**
 * STRUCTURAL: o parser observou um construto (anotação, ciclo de pacote, framework).
 * HEURISTIC: regex, métrica-proxy ou correspondência aproximada — falso positivo e falso negativo esperados.
 * Nenhum dos dois é prova de risco de negócio.
 */
public enum FindingCertainty {
    STRUCTURAL,
    HEURISTIC
}

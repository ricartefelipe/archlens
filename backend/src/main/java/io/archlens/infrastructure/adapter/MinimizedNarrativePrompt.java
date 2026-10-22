package io.archlens.infrastructure.adapter;

/**
 * Texto enviado a um provedor externo. Só contagens agregadas — sem caminho, hash ou trecho.
 */
public final class MinimizedNarrativePrompt {

    private MinimizedNarrativePrompt() {
    }

    public static String build(int findingCount, String dimension, String score) {
        String safeDimension = dimension == null || dimension.isBlank() ? "N/A" : dimension;
        String safeScore = score == null || score.isBlank() ? "0" : score;
        return "Elabore um parágrafo executivo em português a partir apenas destes agregados. "
                + "Achados candidatos: " + findingCount + ". "
                + "Dimensão de maior score: " + safeDimension + " (" + safeScore + "). "
                + "Não afirme falhas comprovadas. Não invente arquivos, CVEs, trechos ou métricas ausentes.";
    }
}

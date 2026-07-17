package io.archlens.analyzer.core.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtil {

    /**
     * Prefixo de domínio para indicadores de segredo. Evita hash SHA-256 “nu” do valor,
     * que permitiria correlacionar o mesmo segredo entre análises distintas pelo hash.
     * O indicador mistura tipo/caminho/linha + valor, sem persistir o valor em claro.
     */
    private static final String SECRET_INDICATOR_DOMAIN = "archlens/secret-indicator/v1";

    private HashUtil() {
    }

    public static String sha256(String input) {
        return "sha256:" + hexSha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Indicador determinístico para a mesma ocorrência (mesmo path/linha/tipo/valor),
     * sem permitir correlacionar o mesmo valor secreto em caminhos ou análises diferentes
     * apenas pelo hash do valor isolado.
     */
    public static String secretOccurrenceIndicator(String type, String relativePath, int line, String secretValue) {
        String material = SECRET_INDICATOR_DOMAIN
                + '\0' + type
                + '\0' + relativePath.replace('\\', '/')
                + '\0' + line
                + '\0' + (secretValue == null ? "" : secretValue);
        return sha256(material);
    }

    private static String hexSha256(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}

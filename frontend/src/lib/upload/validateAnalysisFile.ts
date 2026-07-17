/** Limite alinhado ao backend (`archlens.import.max-bytes`, default 5 MiB). */
export const MAX_ANALYSIS_BYTES = 5_242_880;

export type AnalysisFileValidation =
  | { ok: true; text: string; byteLength: number }
  | { ok: false; message: string };

/**
 * Validação client-side (extensão, tamanho, JSON).
 * Não substitui a validação de schema do servidor.
 */
export async function validateAnalysisFile(
  file: File,
): Promise<AnalysisFileValidation> {
  const name = file.name?.toLowerCase() ?? "";
  if (!name.endsWith(".json")) {
    return {
      ok: false,
      message: "Envie um arquivo .json (analysis.json do analyzer).",
    };
  }

  if (file.size <= 0) {
    return { ok: false, message: "Arquivo vazio." };
  }

  if (file.size > MAX_ANALYSIS_BYTES) {
    return {
      ok: false,
      message: `Arquivo excede o limite de ${MAX_ANALYSIS_BYTES} bytes.`,
    };
  }

  let text: string;
  try {
    text = await file.text();
  } catch {
    return { ok: false, message: "Não foi possível ler o arquivo." };
  }

  const byteLength = new TextEncoder().encode(text).length;
  if (byteLength > MAX_ANALYSIS_BYTES) {
    return {
      ok: false,
      message: `Conteúdo excede o limite de ${MAX_ANALYSIS_BYTES} bytes.`,
    };
  }

  try {
    const parsed: unknown = JSON.parse(text);
    if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {
        ok: false,
        message: "JSON inválido: esperado um objeto na raiz.",
      };
    }
  } catch {
    return { ok: false, message: "Conteúdo não é um JSON válido." };
  }

  return { ok: true, text, byteLength };
}

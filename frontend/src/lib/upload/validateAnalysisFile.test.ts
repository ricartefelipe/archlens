import { describe, expect, it } from "vitest";
import {
  MAX_ANALYSIS_BYTES,
  validateAnalysisFile,
} from "@/lib/upload/validateAnalysisFile";

function fileFrom(
  name: string,
  content: string,
  type = "application/json",
): File {
  return new File([content], name, { type });
}

describe("validateAnalysisFile", () => {
  it("aceita JSON de objeto válido", async () => {
    const result = await validateAnalysisFile(
      fileFrom("analysis.json", JSON.stringify({ schemaVersion: "1.0.0" })),
    );
    expect(result.ok).toBe(true);
  });

  it("rejeita extensão inválida", async () => {
    const result = await validateAnalysisFile(
      fileFrom("analysis.txt", "{}"),
    );
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/\.json/i);
  });

  it("rejeita JSON inválido", async () => {
    const result = await validateAnalysisFile(
      fileFrom("analysis.json", "{not-json"),
    );
    expect(result.ok).toBe(false);
  });

  it("rejeita array na raiz", async () => {
    const result = await validateAnalysisFile(
      fileFrom("analysis.json", "[]"),
    );
    expect(result.ok).toBe(false);
  });

  it("rejeita payload acima do limite", async () => {
    const big = "x".repeat(MAX_ANALYSIS_BYTES + 1);
    const result = await validateAnalysisFile(
      fileFrom("analysis.json", `{"a":"${big}"}`),
    );
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/limite/i);
  });
});

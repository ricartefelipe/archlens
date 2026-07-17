import { expect, type Page } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

export const UNIQUE = `e2e-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

export function analysisFixturePath(): string {
  const candidates = [
    process.env.ARCHLENS_ANALYSIS_JSON ?? "",
    "/tmp/archlens-e2e-analysis.json",
    path.resolve(__dirname, "../../backend/src/test/resources/valid-analysis.json"),
    path.resolve(__dirname, "../../backend/src/test/resources/demo-analysis.json"),
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (fs.existsSync(candidate)) {
      return candidate;
    }
  }
  throw new Error(
    "Fixture analysis.json não encontrada. Gere com o CLI ou use backend/src/test/resources/valid-analysis.json",
  );
}

/**
 * Login local de demo. Contra o frontend Compose, use PLAYWRIGHT_BASE_URL=http://localhost:3000
 * (não 127.0.0.1) para alinhar cookies com NEXTAUTH_URL.
 */
export async function loginAs(
  page: Page,
  username: string,
  password: string,
): Promise<void> {
  await page.goto("/login");
  await page.getByLabel(/Usuário/i).fill(username);
  await page.getByLabel(/Senha/i).fill(password);
  await page.getByRole("button", { name: /^Entrar$/i }).click();
  await page.waitForURL(/\/(assessments)?$/, { timeout: 20_000 });
}

export async function expectLoggedIn(page: Page): Promise<void> {
  await expect(page.getByRole("button", { name: /Sair/i })).toBeVisible({
    timeout: 15_000,
  });
}

export async function logout(page: Page): Promise<void> {
  await page.getByRole("button", { name: /Sair/i }).click();
  await page.waitForURL(/\/login/, { timeout: 15_000 });
}

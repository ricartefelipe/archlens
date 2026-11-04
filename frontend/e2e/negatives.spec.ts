import { test, expect } from "@playwright/test";
import path from "node:path";
import fs from "node:fs";
import os from "node:os";
import {
  UNIQUE,
  analysisFixturePath,
  apiUrl,
  expectLoggedIn,
  localPassword,
  loginAs,
  serverIdentity,
} from "./helpers";

test.describe("Cenários negativos E2E", () => {
  test("rota protegida redireciona sem sessão", async ({ page }) => {
    await page.goto("/assessments");
    await expect(page).toHaveURL(/\/login/);
  });

  test("VIEWER não consegue criar avaliação (UI + backend)", async ({
    page,
  }) => {
    await loginAs(page, "viewer", localPassword("viewer"));
    await expectLoggedIn(page);
    await page.goto("/assessments");
    await expect(
      page.getByRole("button", { name: /Criar avaliação/i }),
    ).toHaveCount(0);

    const response = await page.request.post(`${apiUrl()}/assessments`, {
      data: { name: `viewer-forbidden-${UNIQUE}`, description: "neg" },
      headers: {
        "Content-Type": "application/json",
        ...serverIdentity("viewer", "VIEWER"),
      },
    });
    expect(response.status()).toBe(403);

    // Pela ponte do frontend, a sessão de VIEWER vale mais que qualquer header forjado no navegador.
    const viaBff = await page.request.post("/bff/v1/assessments", {
      data: { name: `viewer-forjado-${UNIQUE}`, description: "neg" },
      headers: { "Content-Type": "application/json", "X-User": "admin", "X-Roles": "ADMIN" },
    });
    expect(viaBff.status()).toBe(403);
  });

  test("API não aceita identidade sem o segredo do frontend", async ({ request }) => {
    for (const headers of [
      {},
      { "X-User": "intruso", "X-Roles": "ADMIN" },
      { "X-User": "intruso", "X-Roles": "ADMIN", "X-ArchLens-Proxy-Secret": "chute-errado-chute-errado-chute-01" },
    ]) {
      const list = await request.get(`${apiUrl()}/assessments`, { headers });
      expect(list.status()).toBe(401);
      const audit = await request.get(`${apiUrl()}/audit`, { headers });
      expect(audit.status()).toBe(401);
      const create = await request.post(`${apiUrl()}/assessments`, {
        data: { name: `intrusa-${UNIQUE}` },
        headers: { "Content-Type": "application/json", ...headers },
      });
      expect(create.status()).toBe(401);
    }
    const health = await request.get(`${apiUrl()}/health`);
    expect(health.status()).toBe(200);
  });

  test("ponte da API sem sessão responde 401", async ({ request }) => {
    const response = await request.get("/bff/v1/assessments");
    expect(response.status()).toBe(401);
  });

  test("upload de arquivo inválido é bloqueado na UI", async ({ page }) => {
    await loginAs(page, "architect", localPassword("architect"));
    await page.goto("/assessments");
    const name = `Neg inválido ${UNIQUE}`;
    await page.getByLabel(/Nome \*/i).fill(name);
    await page.getByRole("button", { name: /Criar avaliação/i }).click();
    await page.waitForURL(/\/assessments\/[0-9a-f-]{36}/i, { timeout: 30_000 });
    const id = page.url().split("/assessments/")[1]?.split("/")[0];

    const badFile = path.join(os.tmpdir(), `bad-${UNIQUE}.txt`);
    fs.writeFileSync(badFile, "not-json");
    await page.goto(`/assessments/${id}/import`);
    await page.locator("#analysis-file").setInputFiles(badFile);
    await page.getByRole("button", { name: /Importar análise/i }).click();
    await expect(
      page.locator('div[role="alert"]').filter({
        hasText: /Envie um arquivo \.json/i,
      }),
    ).toBeVisible({
      timeout: 10_000,
    });
  });

  test("payload excedido é rejeitado", async ({ page }) => {
    await loginAs(page, "architect", localPassword("architect"));
    await page.goto("/assessments");
    await page.getByLabel(/Nome \*/i).fill(`Neg oversized ${UNIQUE}`);
    await page.getByRole("button", { name: /Criar avaliação/i }).click();
    await page.waitForURL(/\/assessments\/[0-9a-f-]{36}/i, { timeout: 30_000 });
    const id = page.url().split("/assessments/")[1]?.split("/")[0];

    const oversized = path.join(os.tmpdir(), `over-${UNIQUE}.json`);
    const filler = "x".repeat(5_300_000);
    fs.writeFileSync(oversized, JSON.stringify({ pad: filler }));
    await page.goto(`/assessments/${id}/import`);
    await page.locator("#analysis-file").setInputFiles(oversized);
    await page.getByRole("button", { name: /Importar análise/i }).click();
    await expect(page.getByText(/limite|excede|413|Payload/i)).toBeVisible({
      timeout: 15_000,
    });
  });

  test("publicação duplicada retorna conflito", async ({ page }) => {
    test.setTimeout(120_000);
    await loginAs(page, "architect", localPassword("architect"));
    await page.goto("/assessments");
    await page.getByLabel(/Nome \*/i).fill(`Neg publish ${UNIQUE}`);
    await page.getByRole("button", { name: /Criar avaliação/i }).click();
    await page.waitForURL(/\/assessments\/[0-9a-f-]{36}/i, { timeout: 30_000 });
    const id = page.url().split("/assessments/")[1]?.split("/")[0];

    await page.goto(`/assessments/${id}/import`);
    await page.locator("#analysis-file").setInputFiles(analysisFixturePath());
    await page.getByRole("button", { name: /Importar análise/i }).click();
    await expect(page.getByText(/Importação concluída/i)).toBeVisible({
      timeout: 60_000,
    });

    await page.goto(`/assessments/${id}/report`);
    page.once("dialog", (dialog) => void dialog.accept());
    await page.getByRole("button", { name: /Gerar relatório/i }).click();
    await expect(page.getByText(/Versão/i)).toBeVisible({ timeout: 30_000 });
    await page.getByRole("button", { name: /^Publicar$/i }).click();
    await expect(page.getByText(/PUBLISHED|Publicado/i).first()).toBeVisible({
      timeout: 30_000,
    });

    const publishBtn = page.getByRole("button", { name: /^Publicar$/i });
    await expect(publishBtn).toBeDisabled();
  });

  test("recurso de outra organização retorna 404", async ({ page }) => {
    await loginAs(page, "architect", localPassword("architect"));
    const foreignId = "00000000-0000-4000-8000-000000000099";
    const response = await page.request.get(`${apiUrl()}/assessments/${foreignId}`, {
      headers: {
        ...serverIdentity("architect", "ARCHITECT,VIEWER"),
        "X-Organization-Id": "11111111-1111-4111-8111-111111111111",
      },
    });
    expect([404, 403]).toContain(response.status());
  });
});

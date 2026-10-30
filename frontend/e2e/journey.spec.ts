import { test, expect } from "@playwright/test";
import {
  UNIQUE,
  analysisFixturePath,
  expectLoggedIn,
  loginAs,
  logout,
} from "./helpers";

/**
 * Jornada E2E principal contra stack real (Postgres + backend + frontend).
 * Requer serviços em localhost:8080 (API) e baseURL do Playwright (frontend).
 */
test.describe.configure({ mode: "serial" });

test.describe("Jornada E2E ArchLens (serviços reais)", () => {
  test("autenticar → assessment → import → inventário → finding → questionário → scores → roadmap → relatório → auditoria → logout", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000);
    const name = `Avaliação ${UNIQUE}`;
    const analysisPath = analysisFixturePath();

    await loginAs(page, "architect", "architect123");
    await expectLoggedIn(page);

    await page.goto("/assessments");
    await page.getByLabel(/Nome \*/i).fill(name);
    await page.getByLabel(/Descrição/i).fill(`E2E ${UNIQUE}`);
    await page.getByRole("button", { name: /Criar avaliação/i }).click();
    await page.waitForURL(/\/assessments\/[0-9a-f-]{36}/i, {
      timeout: 30_000,
    });
    const assessmentUrl = page.url();
    const assessmentId = assessmentUrl.split("/assessments/")[1]?.split("/")[0];
    expect(assessmentId).toBeTruthy();

    await page.goto(`/assessments/${assessmentId}/import`);
    await page.locator("#analysis-file").setInputFiles(analysisPath);
    await page.getByRole("button", { name: /Importar análise/i }).click();
    await expect(
      page.getByText(/Importação concluída/i),
    ).toBeVisible({ timeout: 60_000 });

    await page.goto(`/assessments/${assessmentId}/inventory`);
    await expect(page.getByRole("heading", { name: /Inventário/i })).toBeVisible();
    await expect(page.getByText(/Módulos/i).first()).toBeVisible();
    await expect(
      page.locator("table").first().locator("tbody tr").first(),
    ).toBeVisible({ timeout: 30_000 });

    await page.goto(`/assessments/${assessmentId}/findings`);
    await expect(page.getByRole("heading", { name: "Achados" })).toBeVisible();
    const statusSelect = page.locator('select[id^="status-"]').first();
    await expect(statusSelect).toBeVisible({ timeout: 30_000 });
    await statusSelect.selectOption("APPROVED");
    await expect(statusSelect).toHaveValue("APPROVED");

    await page.goto(`/assessments/${assessmentId}/questionnaire`);
    const numberInputs = page.locator('input[type="number"]');
    const count = await numberInputs.count();
    expect(count).toBeGreaterThan(0);
    for (let i = 0; i < count; i += 1) {
      await numberInputs.nth(i).fill(String(3 + (i % 3)));
    }
    await page.getByRole("button", { name: /Salvar questionário/i }).click();
    await expect(page.getByText(/Questionário salvo/i)).toBeVisible({
      timeout: 20_000,
    });

    await page.goto(`/assessments/${assessmentId}/dimensions`);
    await expect(page.getByRole("heading", { name: /Dimens/i })).toBeVisible();
    await expect(page.getByText(/rationale|Score|score|dimens/i).first()).toBeVisible({
      timeout: 20_000,
    });

    await page.goto(`/assessments/${assessmentId}/roadmap`);
    await page.getByRole("button", { name: /Adicionar item/i }).click();
    await page.locator('input[id^="title-"]').last().fill(`Item ${UNIQUE}`);
    await page
      .locator('textarea[id^="desc-"]')
      .last()
      .fill("Modernizar camada legada");
    await page.locator('select[id^="phase-"]').last().selectOption("SHORT_TERM");
    await page.getByRole("button", { name: /Salvar roadmap/i }).click();
    await expect(page.getByText(/Roadmap salvo/i)).toBeVisible({
      timeout: 20_000,
    });

    await page.goto(`/assessments/${assessmentId}/report`);
    page.once("dialog", (dialog) => void dialog.accept());
    await page.getByRole("button", { name: /Gerar relatório/i }).click();
    await expect(page.getByText(/Versão/i)).toBeVisible({ timeout: 30_000 });

    const fs = await import("node:fs");
    const pdfDownloadPromise = page.waitForEvent("download", {
      timeout: 60_000,
    });
    await page.getByRole("button", { name: /Baixar PDF/i }).click();
    const pdfDownload = await pdfDownloadPromise;
    const pdfPath = testInfo.outputPath(`report-${UNIQUE}.pdf`);
    await pdfDownload.saveAs(pdfPath);
    expect(fs.statSync(pdfPath).size).toBeGreaterThan(100);
    expect(pdfDownload.suggestedFilename()).toMatch(/\.pdf$/i);

    const htmlDownloadPromise = page.waitForEvent("download", {
      timeout: 60_000,
    });
    await page.getByRole("button", { name: /Baixar HTML/i }).click();
    const htmlDownload = await htmlDownloadPromise;
    const htmlPath = testInfo.outputPath(`report-${UNIQUE}.html`);
    await htmlDownload.saveAs(htmlPath);
    expect(fs.statSync(htmlPath).size).toBeGreaterThan(50);

    await page.getByRole("button", { name: /^Publicar$/i }).click();
    await expect(page.getByText(/PUBLISHED|Publicado/i).first()).toBeVisible({
      timeout: 30_000,
    });

    await page.goto(`/assessments/${assessmentId}/audit`);
    await expect(page.getByRole("heading", { name: /Auditoria/i })).toBeVisible();
    await expect(
      page.getByText(/IMPORT|QUESTION|REPORT|PUBLISH|FINDING|ROADMAP/i).first(),
    ).toBeVisible({ timeout: 20_000 });

    await expect(page.getByRole("button", { name: /Excluir avaliação/i })).toHaveCount(0);

    await logout(page);
    await expect(page).toHaveURL(/\/login/);

    await loginAs(page, "admin", "admin123");
    await page.goto(`/assessments/${assessmentId}`);
    page.once("dialog", (dialog) => void dialog.dismiss());
    await page.getByRole("button", { name: /Excluir avaliação/i }).click();
    await expect(page).toHaveURL(new RegExp(`/assessments/${assessmentId}$`));

    page.once("dialog", (dialog) => void dialog.accept());
    await page.getByRole("button", { name: /Excluir avaliação/i }).click();
    await page.waitForURL(/\/assessments\/?$/, { timeout: 30_000 });

    const removed = await page.request.get(
      `${process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1"}/assessments/${assessmentId}`,
      { headers: { "X-User": "admin", "X-Roles": "ADMIN" } },
    );
    expect(removed.status()).toBe(404);
  });
});

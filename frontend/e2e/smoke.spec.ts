import { test, expect } from "@playwright/test";

test.describe("Landing pública", () => {
  test("exibe branding ArchLens e CTA de login", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      /ArchLens|Diagnóstico arquitetural/i,
    );
    await expect(page.getByRole("link", { name: /Entrar/i })).toBeVisible();
  });

  test("página de login renderiza formulário", async ({ page }) => {
    await page.goto("/login");
    await expect(
      page.getByRole("heading", { name: /Entrar no ArchLens/i }),
    ).toBeVisible();
  });
});

test.describe("Rotas protegidas", () => {
  test("assessments redireciona para login sem sessão", async ({ page }) => {
    await page.goto("/assessments");
    await expect(page).toHaveURL(/\/login/);
  });
});

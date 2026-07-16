import { test, expect } from "@playwright/test";

/**
 * Smoke tests do frontend ArchLens.
 *
 * Sem stack completa (Keycloak + backend), os testes de login OIDC são ignorados.
 * Para executar com stack local:
 *   1. Suba infra (Keycloak) e backend
 *   2. Configure .env.local
 *   3. npm run test:e2e
 *
 * Para smoke rápido sem serviços:
 *   PLAYWRIGHT_SKIP_WEBSERVER=1 npm run dev &
 *   PLAYWRIGHT_SKIP_WEBSERVER=1 npm run test:e2e
 */

const hasKeycloak =
  Boolean(process.env.KEYCLOAK_ISSUER) &&
  Boolean(process.env.KEYCLOAK_CLIENT_ID);

test.describe("Landing pública", () => {
  test("exibe branding ArchLens e CTA de login", async ({ page }) => {
    await page.goto("/");
    await expect(page.getByRole("heading", { level: 1 })).toContainText(
      /ArchLens|Diagnóstico arquitetural/i,
    );
    await expect(page.getByRole("link", { name: /Entrar/i })).toBeVisible();
  });

  test("página de login renderiza instruções Keycloak", async ({ page }) => {
    await page.goto("/login");
    await expect(page.getByRole("heading", { name: /Entrar no ArchLens/i })).toBeVisible();
    await expect(page.getByText(/Keycloak/i)).toBeVisible();
  });
});

test.describe("Login OIDC", () => {
  test.skip(!hasKeycloak, "Keycloak não configurado — defina KEYCLOAK_* no ambiente");

  test("redireciona para Keycloak ao clicar em entrar", async ({ page }) => {
    await page.goto("/login");
    await page.getByRole("button", { name: /Entrar com Keycloak/i }).click();
    await page.waitForURL(/realms\/archlens|openid-connect/i, { timeout: 15_000 });
  });
});

test.describe("Rotas protegidas", () => {
  test("assessments redireciona para login sem sessão", async ({ page }) => {
    await page.goto("/assessments");
    await expect(page).toHaveURL(/\/login/);
  });
});

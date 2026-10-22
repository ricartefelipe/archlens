import { test, expect } from "@playwright/test";

/**
 * Autenticação real no Keycloak via Authorization Code.
 * Execute com ARCHLENS_E2E_OIDC=1, OIDC_E2E_USERNAME e OIDC_E2E_PASSWORD
 * somente em ambiente isolado com usuários de demonstração.
 */
test("login OIDC e acesso à API protegida", async ({ page, request }) => {
  test.skip(process.env.ARCHLENS_E2E_OIDC !== "1", "Exige Keycloak e backend com OIDC real ativo");

  const username = process.env.OIDC_E2E_USERNAME;
  const password = process.env.OIDC_E2E_PASSWORD;
  if (!username || !password) {
    throw new Error("Defina OIDC_E2E_USERNAME e OIDC_E2E_PASSWORD para o teste integrado");
  }

  await page.goto("/login");
  await page.getByRole("button", { name: "Entrar com Keycloak" }).click();

  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator("#kc-login").click();

  await page.waitForURL(/\/assessments(?:\?|$)/, { timeout: 45_000 });
  await expect(page.getByRole("heading", { name: "Avaliações" })).toBeVisible();
  await expect(page.getByRole("alert")).toHaveCount(0); // Sem erro de autorização na chamada real à API.

  const api = process.env.OIDC_E2E_API_URL ?? "http://127.0.0.1:8080/api/v1/assessments";
  const withoutToken = await request.get(api);
  expect(withoutToken.status()).toBe(401);

  const forgedHeaders = await request.get(api, {
    headers: { "X-User": "admin", "X-Roles": "ADMIN", "X-Organization-Id": "00000000-0000-0000-0000-000000000001" },
  });
  expect(forgedHeaders.status()).toBe(401);
});

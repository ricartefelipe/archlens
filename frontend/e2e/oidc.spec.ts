import { test, expect, type APIRequestContext } from "@playwright/test";

/**
 * Autenticação real no Keycloak via Authorization Code com PKCE.
 * Execute com ARCHLENS_E2E_OIDC=1, OIDC_E2E_USERNAME e OIDC_E2E_PASSWORD
 * somente em ambiente isolado com usuários de demonstração.
 *
 * Com OIDC_E2E_KC_ADMIN_URL (ex.: http://localhost:8081), OIDC_E2E_KC_ADMIN_USER e
 * OIDC_E2E_KC_ADMIN_PASSWORD, o teste também prova a renovação do access token depois
 * de expirar e a falha fechada quando a sessão é revogada no IdP. Para isso o realm
 * precisa de um access token curto (OIDC_E2E_ACCESS_TOKEN_SECONDS, padrão 40).
 */
const api = process.env.OIDC_E2E_API_URL ?? "http://127.0.0.1:8080/api/v1/assessments";
const kcAdminUrl = process.env.OIDC_E2E_KC_ADMIN_URL;
const tokenSeconds = Number(process.env.OIDC_E2E_ACCESS_TOKEN_SECONDS ?? "40");

test.describe.configure({ mode: "serial" });

test("login OIDC e acesso à API protegida", async ({ page, request }) => {
  test.skip(process.env.ARCHLENS_E2E_OIDC !== "1", "Exige Keycloak e backend com OIDC real ativo");

  const username = process.env.OIDC_E2E_USERNAME;
  const password = process.env.OIDC_E2E_PASSWORD;
  if (!username || !password) {
    throw new Error("Defina OIDC_E2E_USERNAME e OIDC_E2E_PASSWORD para o teste integrado");
  }

  await page.goto("/login");
  await page.getByRole("button", { name: "Entrar com Keycloak" }).click();

  // O redirecionamento ao IdP leva o code_challenge: é Authorization Code com PKCE, não implícito.
  await page.waitForURL(/openid-connect\/auth/, { timeout: 30_000 });
  const authorizeUrl = new URL(page.url());
  expect(authorizeUrl.searchParams.get("response_type")).toBe("code");
  expect(authorizeUrl.searchParams.get("code_challenge_method")).toBe("S256");
  expect(authorizeUrl.searchParams.get("code_challenge")).toBeTruthy();

  await page.locator("#username").fill(username);
  await page.locator("#password").fill(password);
  await page.locator("#kc-login").click();

  await page.waitForURL(/\/assessments(?:\?|$)/, { timeout: 45_000 });
  await expect(page.getByRole("heading", { name: "Avaliações", exact: true })).toBeVisible();
  // Sem erro de autorização na chamada real à API. O anunciador de rotas do Next.js também usa
  // role="alert", por isso a verificação olha só o conteúdo principal.
  await expect(page.locator('main [role="alert"]')).toHaveCount(0);

  const withoutToken = await request.get(api);
  expect(withoutToken.status()).toBe(401);

  const forgedHeaders = await request.get(api, {
    headers: { "X-User": "admin", "X-Roles": "ADMIN", "X-Organization-Id": "00000000-0000-0000-0000-000000000001" },
  });
  expect(forgedHeaders.status()).toBe(401);

  test.skip(!kcAdminUrl, "Renovação e revogação exigem OIDC_E2E_KC_ADMIN_URL");

  // Depois de o access token expirar, a sessão continua válida porque o refresh token o renova.
  await page.waitForTimeout((tokenSeconds + 5) * 1000);
  await page.goto("/assessments");
  await expect(page.getByRole("heading", { name: "Avaliações", exact: true })).toBeVisible();
  await expect(page.locator('main [role="alert"]')).toHaveCount(0);

  // Revogar as sessões do usuário no IdP invalida o refresh token: ao expirar o access token,
  // a aplicação não consegue renovar e volta ao login (falha fechada), sem acesso residual.
  await revokeUserSessions(request, username);
  await page.waitForTimeout((tokenSeconds + 5) * 1000);
  await page.goto("/assessments");
  await page.waitForURL(/\/login/, { timeout: 30_000 });
  await expect(page.getByRole("button", { name: "Entrar com Keycloak" })).toBeVisible();
});

async function revokeUserSessions(request: APIRequestContext, username: string): Promise<void> {
  const adminUser = process.env.OIDC_E2E_KC_ADMIN_USER ?? "admin";
  const adminPassword = process.env.OIDC_E2E_KC_ADMIN_PASSWORD ?? "admin";
  const tokenResponse = await request.post(`${kcAdminUrl}/realms/master/protocol/openid-connect/token`, {
    form: { grant_type: "password", client_id: "admin-cli", username: adminUser, password: adminPassword },
  });
  expect(tokenResponse.ok()).toBeTruthy();
  const adminToken = (await tokenResponse.json()).access_token as string;
  const headers = { Authorization: `Bearer ${adminToken}` };
  const users = await request.get(`${kcAdminUrl}/admin/realms/archlens/users?username=${username}&exact=true`, { headers });
  const [user] = (await users.json()) as Array<{ id: string }>;
  expect(user?.id).toBeTruthy();
  const logout = await request.post(`${kcAdminUrl}/admin/realms/archlens/users/${user.id}/logout`, { headers });
  expect(logout.ok()).toBeTruthy();
}

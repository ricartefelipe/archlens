import { defineConfig, devices } from "@playwright/test";

const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:3000";
const realStack = process.env.ARCHLENS_E2E_REAL === "1";

export default defineConfig({
  testDir: "./e2e",
  fullyParallel: !realStack,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: realStack || process.env.CI ? 1 : undefined,
  reporter: "list",
  timeout: realStack ? 180_000 : 60_000,
  use: {
    baseURL,
    trace: "on-first-retry",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: process.env.PLAYWRIGHT_SKIP_WEBSERVER
    ? undefined
    : {
        command: "npm run dev",
        url: baseURL,
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
        env: {
          ...process.env,
          AUTH_LOCAL_ENABLED: process.env.AUTH_LOCAL_ENABLED ?? "true",
          ARCHLENS_ALLOW_DEMO_CREDENTIALS:
            process.env.ARCHLENS_ALLOW_DEMO_CREDENTIALS ?? "true",
          ARCHLENS_PROFILE_LABEL: process.env.ARCHLENS_PROFILE_LABEL ?? "demo",
          AUTH_LOCAL_ADMIN_PASSWORD:
            process.env.AUTH_LOCAL_ADMIN_PASSWORD ?? "admin123",
          AUTH_LOCAL_ARCHITECT_PASSWORD:
            process.env.AUTH_LOCAL_ARCHITECT_PASSWORD ?? "architect123",
          AUTH_LOCAL_VIEWER_PASSWORD:
            process.env.AUTH_LOCAL_VIEWER_PASSWORD ?? "viewer123",
        },
      },
});

import { signIn, isKeycloakConfigured } from "@/auth";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";

export default async function LoginPage({
  searchParams,
}: {
  searchParams: Promise<{ callbackUrl?: string; error?: string }>;
}) {
  const params = await searchParams;
  const keycloak = isKeycloakConfigured();

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top,#dbeafe,#f8fafc_55%)] px-4">
      <Card className="w-full max-w-md">
        <CardHeader
          title="ArchLens"
          description="Diagnóstico arquitetural local — autenticação OIDC ou login local de desenvolvimento."
        />
        <CardBody className="space-y-4">
          {params.error ? (
            <p className="text-sm text-red-700" role="alert">
              Falha na autenticação. Verifique usuário/senha.
            </p>
          ) : null}

          <form
            action={async (formData) => {
              "use server";
              await signIn("local", {
                username: String(formData.get("username") ?? ""),
                password: String(formData.get("password") ?? ""),
                redirectTo: params.callbackUrl ?? "/assessments",
              });
            }}
            className="space-y-3"
          >
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700">Usuário</span>
              <input
                name="username"
                required
                defaultValue="architect"
                className="w-full rounded-md border border-slate-300 px-3 py-2"
              />
            </label>
            <label className="block text-sm">
              <span className="mb-1 block font-medium text-slate-700">Senha</span>
              <input
                name="password"
                type="password"
                required
                defaultValue="architect123"
                className="w-full rounded-md border border-slate-300 px-3 py-2"
              />
            </label>
            <Button type="submit" className="w-full">
              Entrar
            </Button>
          </form>

          {keycloak ? (
            <form
              action={async () => {
                "use server";
                await signIn("keycloak", {
                  redirectTo: params.callbackUrl ?? "/",
                });
              }}
            >
              <Button type="submit" className="w-full" variant="secondary">
                Entrar com Keycloak
              </Button>
            </form>
          ) : null}

          <details className="text-sm text-slate-600">
            <summary className="cursor-pointer font-medium">
              Credenciais locais (dev)
            </summary>
            <ul className="mt-2 space-y-1 pl-4">
              <li>
                <strong>admin</strong> / admin123 — ADMIN
              </li>
              <li>
                <strong>architect</strong> / architect123 — ARCHITECT
              </li>
              <li>
                <strong>viewer</strong> / viewer123 — VIEWER
              </li>
            </ul>
          </details>
        </CardBody>
      </Card>
    </div>
  );
}

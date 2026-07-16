import Link from "next/link";
import { auth, isAuthConfigured } from "@/auth";
import { AppShell } from "@/components/layout/AppShell";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { getOrganizationSummary, listAssessments } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { formatDate } from "@/lib/utils";
import { Badge } from "@/components/ui/Badge";

export default async function HomePage() {
  const authSession = await auth();

  if (!authSession) {
    return (
      <div className="min-h-screen bg-[radial-gradient(circle_at_top,#dbeafe,#f8fafc_50%)]">
        <header className="border-b border-slate-200/80 bg-white/80 backdrop-blur">
          <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
            <div className="flex items-center gap-2">
              <span className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white">
                AL
              </span>
              <span className="text-lg font-semibold text-slate-900">ArchLens</span>
            </div>
            <Button href="/login">Entrar</Button>
          </div>
        </header>
        <main className="mx-auto max-w-4xl px-4 py-16 sm:px-6">
          <p className="text-sm font-semibold uppercase tracking-wide text-brand-700">ArchLens</p>
          <h1 className="mt-2 text-4xl font-bold tracking-tight text-slate-900">
            Diagnóstico arquitetural local para Java legado
          </h1>
          <p className="mt-4 text-lg text-slate-600">
            Transforme um sistema Java pouco conhecido em diagnóstico rastreável e plano
            executável — sem enviar código-fonte para fora.
          </p>
          <ul className="mt-8 space-y-3 text-slate-700">
            <li>Scan CLI read-only com saída determinística</li>
            <li>Revisão colaborativa de achados com papéis RBAC</li>
            <li>Dimensões, roadmap e relatório HTML/PDF publicável</li>
          </ul>
          <div className="mt-10">
            <Button href="/login" size="lg">
              Começar
            </Button>
          </div>
        </main>
      </div>
    );
  }

  const session = await getSessionContext();
  const authCtx = {
    accessToken: session.accessToken,
    user: session.userName ?? undefined,
    roles: session.roles,
  };
  let orgError: string | null = null;
  let summary = null;
  let recentAssessments: Awaited<ReturnType<typeof listAssessments>>["items"] = [];

  try {
    [summary, recentAssessments] = await Promise.all([
      getOrganizationSummary(authCtx),
      listAssessments(authCtx).then((r) => r.items.slice(0, 5)),
    ]);
  } catch (err) {
    orgError =
      err instanceof Error
        ? err.message
        : "Backend indisponível. Inicie a API em localhost:8080.";
  }

  return (
    <AppShell>
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Dashboard da organização</h1>
          <p className="mt-1 text-slate-600">
            Visão geral das avaliações arquiteturais em andamento.
          </p>
        </div>

        {orgError ? (
          <ErrorState
            title="API indisponível"
            message={orgError}
          />
        ) : summary ? (
          <div className="grid gap-4 sm:grid-cols-3">
            <Card>
              <CardBody>
                <p className="text-sm text-slate-500">Organização</p>
                <p className="mt-1 text-xl font-semibold">{summary.name}</p>
              </CardBody>
            </Card>
            <Card>
              <CardBody>
                <p className="text-sm text-slate-500">Total de avaliações</p>
                <p className="mt-1 text-xl font-semibold">{summary.assessmentCount}</p>
              </CardBody>
            </Card>
            <Card>
              <CardBody>
                <p className="text-sm text-slate-500">Ativas</p>
                <p className="mt-1 text-xl font-semibold">{summary.activeAssessments}</p>
              </CardBody>
            </Card>
          </div>
        ) : null}

        <Card>
          <CardHeader
            title="Avaliações recentes"
            action={<Button href="/assessments" variant="secondary" size="sm">Ver todas</Button>}
          />
          <CardBody>
            {recentAssessments.length === 0 ? (
              <p className="text-sm text-slate-600">
                Nenhuma avaliação ainda.{" "}
                <Link href="/assessments" className="text-brand-700 underline">
                  Criar a primeira
                </Link>
              </p>
            ) : (
              <ul className="divide-y divide-slate-100">
                {recentAssessments.map((assessment) => (
                  <li key={assessment.id} className="flex items-center justify-between py-3">
                    <div>
                      <Link
                        href={`/assessments/${assessment.id}`}
                        className="font-medium text-brand-700 hover:underline"
                      >
                        {assessment.name}
                      </Link>
                      <p className="text-xs text-slate-500">
                        Atualizado {formatDate(assessment.updatedAt)}
                      </p>
                    </div>
                    <Badge label={assessment.status} />
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>

        {!isAuthConfigured() ? (
          <p className="text-sm text-amber-700" role="status">
            Keycloak não configurado. Defina variáveis em <code>.env.local</code> para OIDC real.
          </p>
        ) : null}
      </div>
    </AppShell>
  );
}

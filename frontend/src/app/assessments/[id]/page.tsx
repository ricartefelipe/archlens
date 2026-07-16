import { notFound } from "next/navigation";
import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { Badge } from "@/components/ui/Badge";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";
import { getAssessment } from "@/lib/api/assessments";
import { ApiClientError } from "@/lib/api/client";
import { getSessionContext } from "@/lib/auth/session";
import { formatDate } from "@/lib/utils";

export async function generateMetadata({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return { title: `Avaliação ${id.slice(0, 8)}` };
}

export default async function AssessmentOverviewPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
    const session = await getSessionContext();
  const auth = {
    accessToken: session.accessToken,
    user: session.userName ?? undefined,
    roles: session.roles,
  };

  let assessment = null;
  let error: string | null = null;

  try {
    assessment = await getAssessment(id, auth);
  } catch (err) {
    if (err instanceof ApiClientError && err.status === 404) {
      notFound();
    }
    error = err instanceof Error ? err.message : "Erro ao carregar avaliação.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />

      {error ? (
        <ErrorState message={error} />
      ) : assessment ? (
        <div className="space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h1 className="text-2xl font-bold text-slate-900">{assessment.name}</h1>
              <p className="mt-1 text-slate-600">
                {assessment.description ?? "Sem descrição."}
              </p>
            </div>
            <Badge label={assessment.status} />
          </div>

          <Card>
            <CardHeader title="Resumo" />
            <CardBody>
              <dl className="grid gap-4 sm:grid-cols-2">
                <div>
                  <dt className="text-sm text-slate-500">Projeto alvo</dt>
                  <dd className="font-medium">
                    {assessment.targetProjectName ?? "—"}
                  </dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500">Criado em</dt>
                  <dd>{formatDate(assessment.createdAt)}</dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500">Atualizado em</dt>
                  <dd>{formatDate(assessment.updatedAt)}</dd>
                </div>
                <div>
                  <dt className="text-sm text-slate-500">Criado por</dt>
                  <dd>{assessment.createdBy}</dd>
                </div>
              </dl>
            </CardBody>
          </Card>

          <div className="flex flex-wrap gap-3">
            <Button href={`/assessments/${id}/import`}>Importar scan</Button>
            <Button href={`/assessments/${id}/findings`} variant="secondary">
              Ver achados
            </Button>
            <Button href={`/assessments/${id}/report`} variant="secondary">
              Relatório
            </Button>
          </div>
        </div>
      ) : null}
    </AppShell>
  );
}

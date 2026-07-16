import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { MermaidDiagram } from "@/components/mermaid/MermaidDiagram";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { getDimensions } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";

export const metadata = { title: "Dimensões" };

export default async function DimensionsPage({
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
  const roles = session.roles;
  let error: string | null = null;
  let dimensions: Awaited<ReturnType<typeof getDimensions>> = [];

  try {
    dimensions = await getDimensions(id, auth);
  } catch (err) {
    error = err instanceof Error ? err.message : "Erro ao carregar dimensões.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-2 text-2xl font-bold text-slate-900">Dimensões e scores</h1>
      <p className="mb-6 text-sm text-slate-600">
        Pontuação por dimensão arquitetural com explicação e diagrama Mermaid.
      </p>

      {error ? (
        <ErrorState message={error} />
      ) : dimensions.length === 0 ? (
        <p className="text-sm text-slate-600">
          Nenhuma dimensão calculada. Complete importação, achados e questionário.
        </p>
      ) : (
        <div className="space-y-6">
          {dimensions.map((dimension) => (
            <Card key={dimension.id}>
              <CardHeader
                title={dimension.name}
                description={`Score: ${dimension.score}/${dimension.maxScore} (peso ${dimension.weight})`}
              />
              <CardBody className="space-y-4">
                <p className="text-sm text-slate-700">{dimension.explanation}</p>

                {dimension.factors.length > 0 ? (
                  <ul className="list-disc space-y-1 pl-5 text-sm text-slate-600">
                    {dimension.factors.map((factor) => (
                      <li key={factor.name}>
                        <strong>{factor.name}</strong> ({factor.impact}):{" "}
                        {factor.description}
                      </li>
                    ))}
                  </ul>
                ) : null}

                <MermaidDiagram
                  chart={
                    dimension.mermaidDiagram ??
                    `flowchart LR\n  A[${dimension.name}] --> B[Score ${dimension.score}]`
                  }
                  fallbackText={`Dimensão ${dimension.name}: ${dimension.score}/${dimension.maxScore}. ${dimension.explanation}`}
                />
              </CardBody>
            </Card>
          ))}
        </div>
      )}
    </AppShell>
  );
}

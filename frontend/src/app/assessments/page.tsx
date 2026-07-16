import Link from "next/link";
import { AppShell } from "@/components/layout/AppShell";
import { CreateAssessmentForm } from "@/components/assessments/CreateAssessmentForm";
import { Badge } from "@/components/ui/Badge";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { Button } from "@/components/ui/Button";
import { listAssessments } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { canWrite } from "@/lib/auth/roles";
import { formatDate } from "@/lib/utils";

export const metadata = { title: "Avaliações" };

export default async function AssessmentsPage() {
    const session = await getSessionContext();
  const auth = {
    accessToken: session.accessToken,
    user: session.userName ?? undefined,
    roles: session.roles,
  };
  const roles = session.roles;

  let error: string | null = null;
  let assessments: Awaited<ReturnType<typeof listAssessments>>["items"] = [];

  try {
    assessments = (await listAssessments(auth)).items;
  } catch (err) {
    error =
      err instanceof Error ? err.message : "Não foi possível listar avaliações.";
  }

  return (
    <AppShell>
      <div className="space-y-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900">Avaliações</h1>
            <p className="mt-1 text-slate-600">
              Gerencie diagnósticos arquiteturais da organização.
            </p>
          </div>
          <Button href="/" variant="ghost" size="sm">
            Voltar ao início
          </Button>
        </div>

        {error ? <ErrorState message={error} /> : null}

        <Card>
          <CardHeader title="Lista de avaliações" />
          <CardBody>
            {assessments.length === 0 ? (
              <p className="text-sm text-slate-600">
                Nenhuma avaliação cadastrada.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="min-w-full text-left text-sm">
                  <thead className="text-slate-500">
                    <tr>
                      <th scope="col" className="px-3 py-2 font-medium">
                        Nome
                      </th>
                      <th scope="col" className="px-3 py-2 font-medium">
                        Status
                      </th>
                      <th scope="col" className="px-3 py-2 font-medium">
                        Atualizado
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {assessments.map((assessment) => (
                      <tr key={assessment.id} className="border-t border-slate-100">
                        <td className="px-3 py-3">
                          <Link
                            href={`/assessments/${assessment.id}`}
                            className="font-medium text-brand-700 hover:underline"
                          >
                            {assessment.name}
                          </Link>
                        </td>
                        <td className="px-3 py-3">
                          <Badge label={assessment.status} />
                        </td>
                        <td className="px-3 py-3 text-slate-600">
                          {formatDate(assessment.updatedAt)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardBody>
        </Card>

        {canWrite(roles) ? <CreateAssessmentForm /> : null}
      </div>
    </AppShell>
  );
}

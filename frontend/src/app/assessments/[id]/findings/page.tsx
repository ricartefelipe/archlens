import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { FindingsPanel } from "@/components/assessments/FindingsPanel";
import { ErrorState } from "@/components/ui/ErrorState";
import { listFindings } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { canManageFindings } from "@/lib/auth/roles";

export const metadata = { title: "Achados" };

export default async function FindingsPage({
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
  let findings: Awaited<ReturnType<typeof listFindings>>["items"] = [];

  try {
    findings = (await listFindings(id, {}, auth)).items;
  } catch (err) {
    error = err instanceof Error ? err.message : "Erro ao carregar achados.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-2 text-2xl font-bold text-slate-900">Achados</h1>
      <p className="mb-6 text-sm text-slate-600">
        Revise findings gerados pelas regras. Alteração de status requer papel ARCHITECT ou ADMIN.
      </p>

      {error ? (
        <ErrorState message={error} />
      ) : (
        <FindingsPanel
          assessmentId={id}
          initialFindings={findings}
          canEdit={canManageFindings(roles)}
          auth={auth}
        />
      )}
    </AppShell>
  );
}

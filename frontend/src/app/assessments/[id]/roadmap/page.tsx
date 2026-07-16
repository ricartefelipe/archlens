import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { RoadmapEditor } from "@/components/assessments/RoadmapEditor";
import { ErrorState } from "@/components/ui/ErrorState";
import { getRoadmap } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { canWrite } from "@/lib/auth/roles";

export const metadata = { title: "Roadmap" };

export default async function RoadmapPage({
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
  let items: Awaited<ReturnType<typeof getRoadmap>> = [];

  try {
    items = await getRoadmap(id, auth);
  } catch (err) {
    error = err instanceof Error ? err.message : "Erro ao carregar roadmap.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-2 text-2xl font-bold text-slate-900">Opções e roadmap</h1>
      <p className="mb-6 text-sm text-slate-600">
        Priorize iniciativas de modernização vinculadas aos achados.
      </p>

      {error ? <ErrorState message={error} /> : null}

      <RoadmapEditor
        assessmentId={id}
        initialItems={items}
        canEdit={canWrite(roles)}
      />
    </AppShell>
  );
}

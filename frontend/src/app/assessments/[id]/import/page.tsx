import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { ImportPanel } from "@/components/assessments/ImportPanel";
import { getSessionContext } from "@/lib/auth/session";

export const metadata = { title: "Importar scan" };

export default async function ImportPage({
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
  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Importar analysis.json</h1>
      <ImportPanel assessmentId={id} auth={auth} />
    </AppShell>
  );
}

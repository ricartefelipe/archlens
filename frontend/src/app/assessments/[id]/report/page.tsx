import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { ReportPanel } from "@/components/assessments/ReportPanel";
import { listReports } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { canWrite } from "@/lib/auth/roles";

export const metadata = { title: "Relatório" };

export default async function ReportPage({
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

  let initialSnapshot = null;
  try {
    const reports = await listReports(id, auth);
    initialSnapshot = reports[0] ?? null;
  } catch {
    initialSnapshot = null;
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Relatório</h1>
      <ReportPanel
        assessmentId={id}
        initialSnapshot={initialSnapshot}
        canEdit={canWrite(roles)}
        auth={auth}
      />
    </AppShell>
  );
}

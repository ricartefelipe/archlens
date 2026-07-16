import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { getAuditEvents } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { formatDate } from "@/lib/utils";

export const metadata = { title: "Auditoria" };

export default async function AuditPage({
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
  let events: Awaited<ReturnType<typeof getAuditEvents>> = [];

  try {
    events = await getAuditEvents(id, auth);
  } catch (err) {
    error = err instanceof Error ? err.message : "Erro ao carregar auditoria.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Auditoria</h1>

      {error ? (
        <ErrorState message={error} />
      ) : (
        <Card>
          <CardHeader title="Timeline de eventos" />
          <CardBody>
            {events.length === 0 ? (
              <p className="text-sm text-slate-600">Nenhum evento registrado.</p>
            ) : (
              <ol className="relative border-l border-slate-200 pl-6">
                {events.map((event) => (
                  <li key={event.id} className="mb-6">
                    <span className="absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full bg-brand-600" />
                    <time className="text-xs text-slate-500">
                      {formatDate(event.timestamp)}
                    </time>
                    <p className="mt-1 font-medium text-slate-900">{event.action}</p>
                    <p className="text-sm text-slate-600">
                      {event.actor} · {event.resourceType}
                      {event.resourceId ? ` #${event.resourceId.slice(0, 8)}` : ""}
                    </p>
                    {event.details ? (
                      <p className="mt-1 text-sm text-slate-500">{event.details}</p>
                    ) : null}
                  </li>
                ))}
              </ol>
            )}
          </CardBody>
        </Card>
      )}
    </AppShell>
  );
}

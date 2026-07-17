import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { getInventory } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";

export const metadata = { title: "Inventário" };

export default async function InventoryPage({
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
  let error: string | null = null;
  let inventory = null;

  try {
    inventory = await getInventory(id, auth);
  } catch (err) {
    error = err instanceof Error ? err.message : "Erro ao carregar inventário.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Inventário</h1>

      {error ? (
        <ErrorState message={error} />
      ) : inventory ? (
        <div className="space-y-6">
          <InventoryTable
            title="Módulos"
            headers={["Nome", "Caminho", "Build"]}
            rows={inventory.modules.map((m) => [m.name, m.path, m.buildSystem])}
          />
          <InventoryTable
            title="Pacotes"
            headers={["Nome", "Arquivos", "Linhas"]}
            rows={inventory.packages.map((p) => [
              p.name,
              String(p.fileCount),
              String(p.lineCount),
            ])}
          />
          <InventoryTable
            title="Tipos"
            headers={["FQN", "Kind", "Linhas"]}
            rows={inventory.types.slice(0, 50).map((t) => [
              t.fqn,
              t.kind,
              String(t.lineCount),
            ])}
            footer={
              inventory.types.length > 50
                ? `Exibindo 50 de ${inventory.types.length} tipos.`
                : undefined
            }
          />
          <InventoryTable
            title="Endpoints"
            headers={["Tipo", "Path/Nome", "Origem"]}
            rows={inventory.endpoints.map((e) => [
              e.type,
              e.pathOrName,
              e.sourcePath,
            ])}
          />
        </div>
      ) : null}
    </AppShell>
  );
}

function InventoryTable({
  title,
  headers,
  rows,
  footer,
}: {
  title: string;
  headers: string[];
  rows: string[][];
  footer?: string;
}) {
  return (
    <Card>
      <CardHeader title={title} />
      <CardBody>
        {rows.length === 0 ? (
          <p className="text-sm text-slate-600">Nenhum registro.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="text-slate-500">
                <tr>
                  {headers.map((header) => (
                    <th key={header} scope="col" className="px-3 py-2 font-medium">
                      {header}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr key={index} className="border-t border-slate-100">
                    {row.map((cell, cellIndex) => (
                      <td key={cellIndex} className="px-3 py-2 text-slate-700">
                        {cell}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {footer ? <p className="mt-2 text-xs text-slate-500">{footer}</p> : null}
      </CardBody>
    </Card>
  );
}

"use client";

import { useCallback, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { ErrorState } from "@/components/ui/ErrorState";
import { LoadingState } from "@/components/ui/LoadingState";
import {
  exportFindingsCsv,
  listFindings,
  type AuthContext,
} from "@/lib/api/assessments";
import { updateFindingStatusAction } from "@/lib/actions/assessments";
import { downloadBlob } from "@/lib/utils";
import type { Finding, FindingSeverity, FindingStatus } from "@/types";

const STATUSES: FindingStatus[] = [
  "OPEN",
  "APPROVED",
  "EDITED",
  "RISK_ACCEPTED",
  "DISMISSED",
];

const SEVERITIES: FindingSeverity[] = [
  "CRITICAL",
  "HIGH",
  "MEDIUM",
  "LOW",
  "INFO",
];

export function FindingsPanel({
  assessmentId,
  canEdit,
  initialFindings,
  auth,
}: {
  assessmentId: string;
  canEdit: boolean;
  initialFindings: Finding[];
  auth?: AuthContext;
}) {
  const [findings, setFindings] = useState(initialFindings);
  const [statusFilter, setStatusFilter] = useState("");
  const [severityFilter, setSeverityFilter] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await listFindings(
        assessmentId,
        {
          status: statusFilter || undefined,
          severity: severityFilter || undefined,
          search: search || undefined,
        },
        auth,
      );
      setFindings(response.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao carregar achados");
    } finally {
      setLoading(false);
    }
  }, [auth, assessmentId, search, severityFilter, statusFilter]);

  async function handleStatusChange(findingId: string, status: FindingStatus) {
    try {
      const updated = await updateFindingStatusAction(assessmentId, findingId, {
        status,
      });
      setFindings((prev) =>
        prev.map((item) => (item.id === findingId ? updated : item)),
      );
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Erro ao atualizar status",
      );
    }
  }

  async function handleExport() {
    setExporting(true);
    setError(null);
    try {
      const blob = await exportFindingsCsv(assessmentId, auth);
      downloadBlob(blob, `findings-${assessmentId}.csv`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao exportar CSV");
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="status-filter" className="block text-sm font-medium">
            Status
          </label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="mt-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">Todos</option>
            {STATUSES.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="severity-filter" className="block text-sm font-medium">
            Severidade
          </label>
          <select
            id="severity-filter"
            value={severityFilter}
            onChange={(e) => setSeverityFilter(e.target.value)}
            className="mt-1 rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">Todas</option>
            {SEVERITIES.map((severity) => (
              <option key={severity} value={severity}>
                {severity}
              </option>
            ))}
          </select>
        </div>
        <div className="min-w-[200px] flex-1">
          <label htmlFor="search" className="block text-sm font-medium">
            Buscar
          </label>
          <input
            id="search"
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Título ou regra…"
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>
        <Button variant="secondary" onClick={() => void reload()} disabled={loading}>
          Filtrar
        </Button>
        <Button variant="secondary" onClick={() => void handleExport()} disabled={exporting}>
          {exporting ? "Exportando…" : "Exportar CSV"}
        </Button>
      </div>

      {error ? <ErrorState message={error} onRetry={() => void reload()} /> : null}
      {loading ? <LoadingState label="Atualizando achados…" /> : null}

      <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
        <table className="min-w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-600">
            <tr>
              <th scope="col" className="px-4 py-3 font-medium">
                Achado
              </th>
              <th scope="col" className="px-4 py-3 font-medium">
                Severidade
              </th>
              <th scope="col" className="px-4 py-3 font-medium">
                Status
              </th>
              {canEdit ? (
                <th scope="col" className="px-4 py-3 font-medium">
                  Ações
                </th>
              ) : null}
            </tr>
          </thead>
          <tbody>
            {findings.length === 0 ? (
              <tr>
                <td colSpan={canEdit ? 4 : 3} className="px-4 py-8 text-slate-500">
                  Nenhum achado encontrado. Importe um scan e execute as regras no backend.
                </td>
              </tr>
            ) : (
              findings.map((finding) => (
                <tr key={finding.id} className="border-t border-slate-100">
                  <td className="px-4 py-3">
                    <p className="font-medium text-slate-900">{finding.title}</p>
                    <p className="mt-1 text-xs text-slate-500">{finding.ruleId}</p>
                    <p className="mt-2 text-slate-600">{finding.description}</p>
                  </td>
                  <td className="px-4 py-3">
                    <Badge label={finding.severity} />
                  </td>
                  <td className="px-4 py-3">
                    <Badge label={finding.status} />
                  </td>
                  {canEdit ? (
                    <td className="px-4 py-3">
                      <label className="sr-only" htmlFor={`status-${finding.id}`}>
                        Alterar status de {finding.title}
                      </label>
                      <select
                        id={`status-${finding.id}`}
                        value={finding.status}
                        onChange={(e) =>
                          void handleStatusChange(
                            finding.id,
                            e.target.value as FindingStatus,
                          )
                        }
                        className="rounded-md border border-slate-300 px-2 py-1 text-sm"
                      >
                        {STATUSES.map((status) => (
                          <option key={status} value={status}>
                            {status}
                          </option>
                        ))}
                      </select>
                    </td>
                  ) : null}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

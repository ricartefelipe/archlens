"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import {
  downloadReport,
  type AuthContext,
} from "@/lib/api/assessments";
import {
  generateReportAction,
  publishReportAction,
} from "@/lib/actions/assessments";
import { downloadBlob, formatDate } from "@/lib/utils";
import type { ReportSnapshot } from "@/types";

export function ReportPanel({
  assessmentId,
  initialSnapshot,
  canEdit,
  auth,
}: {
  assessmentId: string;
  initialSnapshot: ReportSnapshot | null;
  canEdit: boolean;
  auth?: AuthContext;
}) {
  const [snapshot, setSnapshot] = useState(initialSnapshot);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleGenerate() {
    setLoading(true);
    setError(null);
    try {
      setSnapshot(await generateReportAction(assessmentId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao gerar relatório");
    } finally {
      setLoading(false);
    }
  }

  async function handlePublish() {
    setLoading(true);
    setError(null);
    try {
      setSnapshot(await publishReportAction(assessmentId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao publicar relatório");
    } finally {
      setLoading(false);
    }
  }

  async function handleDownload(format: "html" | "pdf") {
    setLoading(true);
    setError(null);
    try {
      const blob = await downloadReport(assessmentId, format, auth);
      downloadBlob(blob, `report-${assessmentId}.${format}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao baixar relatório");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader
          title="Relatório de modernização"
          description="Gere um snapshot imutável em HTML/PDF após revisar achados e roadmap."
        />
        <CardBody className="space-y-4">
          {snapshot ? (
            <dl className="grid gap-2 text-sm sm:grid-cols-2">
              <div>
                <dt className="text-slate-500">Versão</dt>
                <dd className="font-medium">{snapshot.version}</dd>
              </div>
              <div>
                <dt className="text-slate-500">Status</dt>
                <dd>
                  <Badge
                    label={
                      snapshot.status ??
                      (snapshot.published ? "PUBLISHED" : "DRAFT")
                    }
                  />
                </dd>
              </div>
              <div>
                <dt className="text-slate-500">Gerado em</dt>
                <dd>{formatDate(snapshot.generatedAt ?? snapshot.createdAt ?? "")}</dd>
              </div>
              {snapshot.publishedAt ? (
                <div>
                  <dt className="text-slate-500">Publicado em</dt>
                  <dd>{formatDate(snapshot.publishedAt)}</dd>
                </div>
              ) : null}
            </dl>
          ) : (
            <p className="text-sm text-slate-600">
              Nenhum relatório gerado ainda.
            </p>
          )}

          {error ? <ErrorState message={error} /> : null}

          <div className="flex flex-wrap gap-3">
            {canEdit ? (
              <>
                <Button onClick={() => void handleGenerate()} disabled={loading}>
                  {loading ? "Processando…" : "Gerar relatório"}
                </Button>
                <Button
                  variant="secondary"
                  onClick={() => void handlePublish()}
                  disabled={loading || !snapshot}
                >
                  Publicar
                </Button>
              </>
            ) : null}
            <Button
              variant="secondary"
              onClick={() => void handleDownload("html")}
              disabled={loading || !snapshot}
            >
              Baixar HTML
            </Button>
            <Button
              variant="secondary"
              onClick={() => void handleDownload("pdf")}
              disabled={loading || !snapshot}
            >
              Baixar PDF
            </Button>
          </div>
        </CardBody>
      </Card>
    </div>
  );
}

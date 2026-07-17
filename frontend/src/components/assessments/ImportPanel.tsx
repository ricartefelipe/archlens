"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { uploadAnalysisAction } from "@/lib/actions/assessments";
import { getImportStatus, type AuthContext } from "@/lib/api/assessments";
import { ApiClientError } from "@/lib/api/client";
import {
  MAX_ANALYSIS_BYTES,
  validateAnalysisFile,
} from "@/lib/upload/validateAnalysisFile";
import type { ImportJob } from "@/types";

export function ImportPanel({
  assessmentId,
  auth,
}: {
  assessmentId: string;
  auth?: AuthContext;
}) {
  const [file, setFile] = useState<File | null>(null);
  const [job, setJob] = useState<ImportJob | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    if (!job || job.status === "COMPLETED" || job.status === "FAILED") {
      return;
    }

    const interval = setInterval(() => {
      void getImportStatus(assessmentId, auth)
        .then(setJob)
        .catch(() => {
          /* polling errors are non-fatal */
        });
    }, 2000);

    return () => clearInterval(interval);
  }, [auth, assessmentId, job]);

  async function handleUpload(event: React.FormEvent) {
    event.preventDefault();
    if (!file || uploading) {
      return;
    }

    setUploading(true);
    setError(null);

    try {
      const validation = await validateAnalysisFile(file);
      if (!validation.ok) {
        setError(validation.message);
        return;
      }

      const formData = new FormData();
      formData.append("file", file);
      const result = await uploadAnalysisAction(assessmentId, formData);
      setJob(result);
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 403) {
        setError("Você não tem permissão para importar análises.");
      } else if (err instanceof ApiClientError && err.status === 401) {
        setError("Sessão expirada. Faça login novamente.");
      } else {
        setError(
          err instanceof Error
            ? err.message
            : "Falha no upload. Verifique o backend e o formato do JSON.",
        );
      }
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="space-y-6">
      <Card>
        <CardHeader
          title="Instruções CLI"
          description="Execute o scan localmente — o código-fonte não sai da sua máquina."
        />
        <CardBody>
          <ol className="list-decimal space-y-2 pl-5 text-sm text-slate-700">
            <li>Instale o ArchLens CLI (módulo analyzer).</li>
            <li>
              No diretório raiz do projeto Java:
              <pre className="mt-2 overflow-x-auto rounded-md bg-slate-900 p-4 text-xs text-slate-100">
                {`java -jar archlens-cli.jar scan --path /caminho/do/projeto --out analysis.json\njava -jar archlens-cli.jar validate --file analysis.json`}
              </pre>
            </li>
            <li>
              Faça upload do arquivo <code>analysis.json</code> abaixo (máx.{" "}
              {MAX_ANALYSIS_BYTES.toLocaleString("pt-BR")} bytes).
            </li>
          </ol>
        </CardBody>
      </Card>

      <Card>
        <CardHeader title="Upload analysis.json" />
        <CardBody>
          <form onSubmit={(e) => void handleUpload(e)} className="space-y-4">
            <div>
              <label
                htmlFor="analysis-file"
                className="block text-sm font-medium"
              >
                Arquivo JSON *
              </label>
              <input
                id="analysis-file"
                type="file"
                accept="application/json,.json"
                required
                disabled={uploading}
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                className="mt-1 block w-full text-sm text-slate-600 file:mr-4 file:rounded-md file:border-0 file:bg-brand-50 file:px-4 file:py-2 file:text-sm file:font-medium file:text-brand-700"
              />
            </div>
            {error ? <ErrorState message={error} /> : null}
            <Button type="submit" disabled={uploading || !file}>
              {uploading ? "Enviando…" : "Importar análise"}
            </Button>
          </form>
        </CardBody>
      </Card>

      {job ? (
        <Card>
          <CardHeader
            title="Status da importação"
            action={<Badge label={job.status} />}
          />
          <CardBody className="space-y-2 text-sm text-slate-700">
            {job.contentHash ? (
              <p>
                <span className="font-medium">Hash:</span> {job.contentHash}
              </p>
            ) : null}
            {job.errorMessage ? (
              <p className="text-red-700" role="alert">
                {job.errorMessage}
              </p>
            ) : null}
            {job.status === "COMPLETED" ? (
              <p className="text-green-700" role="status">
                Importação concluída. Continue para inventário e achados.
              </p>
            ) : null}
          </CardBody>
        </Card>
      ) : null}
    </div>
  );
}

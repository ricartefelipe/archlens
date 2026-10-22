import { authHeaders, type AuthContext } from "@/lib/api/authHeaders";
import { apiDownload, apiFetch } from "@/lib/api/client";
import type {
  Assessment,
  AuditEvent,
  CreateAssessmentRequest,
  DimensionScore,
  Finding,
  ImportJob,
  Inventory,
  OrganizationSummary,
  Questionnaire,
  ReportSnapshot,
  RoadmapItem,
  RoadmapPhase,
  UpdateFindingStatusRequest,
} from "@/types";

export type { AuthContext };

type AssessmentApi = {
  id: string;
  name: string;
  description?: string;
  status: Assessment["status"];
  projectRootName?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  organizationId?: string;
};

type FindingApi = {
  id: string;
  ruleId: string;
  title: string;
  description: string;
  severity: Finding["severity"];
  status: Finding["status"];
  evidenceIds?: string[];
  dimensionKey?: string;
  reviewNote?: string;
  updatedAt: string;
  reviewedBy?: string;
};

function mapAssessment(a: AssessmentApi): Assessment {
  return {
    id: a.id,
    name: a.name,
    description: a.description,
    status: a.status,
    organizationId: a.organizationId,
    targetProjectName: a.projectRootName,
    projectRootName: a.projectRootName,
    createdAt: a.createdAt,
    updatedAt: a.updatedAt,
    createdBy: a.createdBy,
  };
}

function mapFinding(f: FindingApi, assessmentId: string): Finding {
  return {
    id: f.id,
    assessmentId,
    ruleId: f.ruleId,
    title: f.title,
    description: f.description,
    severity: f.severity,
    status: f.status,
    category: f.dimensionKey ?? "general",
    evidence: (f.evidenceIds ?? []).join(", "),
    recommendation: f.reviewNote,
    updatedAt: f.updatedAt,
    updatedBy: f.reviewedBy,
  };
}

function normalizePhase(phase: string | number | undefined): RoadmapPhase {
  const value = String(phase ?? "SHORT_TERM").toUpperCase();
  if (
    value === "SHORT_TERM" ||
    value === "MEDIUM_TERM" ||
    value === "LONG_TERM"
  ) {
    return value;
  }
  if (value === "1" || value === "SHORT") return "SHORT_TERM";
  if (value === "2" || value === "MEDIUM") return "MEDIUM_TERM";
  if (value === "3" || value === "LONG") return "LONG_TERM";
  return "SHORT_TERM";
}

export async function getOrganizationSummary(
  auth?: AuthContext,
): Promise<OrganizationSummary> {
  const s = await apiFetch<{
    id: string;
    name: string;
    assessmentCount: number;
    findingCount?: number;
  }>("/organization/summary", authHeaders(auth));
  return {
    id: s.id,
    name: s.name,
    assessmentCount: s.assessmentCount,
    activeAssessments: s.assessmentCount,
    findingCount: s.findingCount,
  };
}

export async function listAssessments(
  auth?: AuthContext,
): Promise<{ items: Assessment[]; total: number }> {
  const items = await apiFetch<AssessmentApi[]>(
    "/assessments",
    authHeaders(auth),
  );
  const mapped = items.map(mapAssessment);
  return { items: mapped, total: mapped.length };
}

export async function createAssessment(
  data: CreateAssessmentRequest,
  auth?: AuthContext,
): Promise<Assessment> {
  const created = await apiFetch<AssessmentApi>("/assessments", {
    method: "POST",
    body: JSON.stringify({ name: data.name, description: data.description }),
    ...authHeaders(auth),
  });
  return mapAssessment(created);
}

export async function deleteAssessment(
  id: string,
  auth?: AuthContext,
): Promise<void> {
  await apiFetch<void>(`/assessments/${id}`, {
    method: "DELETE",
    ...authHeaders(auth),
  });
}

export async function getAssessment(
  id: string,
  auth?: AuthContext,
): Promise<Assessment> {
  const a = await apiFetch<AssessmentApi>(
    `/assessments/${id}`,
    authHeaders(auth),
  );
  return mapAssessment(a);
}

export async function uploadAnalysis(
  id: string,
  file: File,
  auth?: AuthContext,
  payloadText?: string,
): Promise<ImportJob> {
  const text = payloadText ?? (await file.text());
  const key =
    typeof crypto !== "undefined" && "randomUUID" in crypto
      ? crypto.randomUUID()
      : `idem-${Date.now()}`;
  const base = authHeaders(auth);
  const result = await apiFetch<{
    importId: string;
    contentHash: string;
    importedAt: string;
    findingsCount: number;
  }>(`/assessments/${id}/import`, {
    method: "POST",
    body: text,
    ...base,
    headers: {
      ...(base.headers ?? {}),
      "Idempotency-Key": key,
      "Content-Type": "application/json",
    },
  });
  return {
    id: result.importId,
    assessmentId: id,
    status: "COMPLETED",
    contentHash: result.contentHash,
    completedAt: result.importedAt,
  };
}

export async function getImportStatus(
  id: string,
  auth?: AuthContext,
): Promise<ImportJob> {
  const a = await getAssessment(id, auth);
  const done =
    a.status !== "DRAFT" &&
    a.status !== "IMPORT_PENDING" &&
    a.status !== "IMPORTING";
  return {
    id: `import-${id}`,
    assessmentId: id,
    status: done ? "COMPLETED" : "PENDING",
  };
}

export async function getInventory(
  id: string,
  auth?: AuthContext,
): Promise<Inventory> {
  const raw = await apiFetch<Partial<Inventory>>(
    `/assessments/${id}/inventory`,
    authHeaders(auth),
  );
  return {
    modules: raw.modules ?? [],
    packages: raw.packages ?? [],
    types: raw.types ?? [],
    endpoints: raw.endpoints ?? [],
    persistence: raw.persistence ?? [],
    messaging: raw.messaging ?? [],
  };
}

export async function listFindings(
  id: string,
  params: { status?: string; severity?: string; search?: string } = {},
  auth?: AuthContext,
): Promise<{ items: Finding[]; total: number }> {
  const raw = await apiFetch<FindingApi[]>(
    `/assessments/${id}/findings`,
    authHeaders(auth),
  );

  let items = raw.map((f) => mapFinding(f, id));
  if (params.status) {
    items = items.filter((f) => f.status === params.status);
  }
  if (params.severity) {
    items = items.filter((f) => f.severity === params.severity);
  }
  if (params.search) {
    const s = params.search.toLowerCase();
    items = items.filter(
      (f) =>
        f.title.toLowerCase().includes(s) ||
        f.ruleId.toLowerCase().includes(s) ||
        f.description.toLowerCase().includes(s),
    );
  }
  return { items, total: items.length };
}

export async function updateFindingStatus(
  assessmentId: string,
  findingId: string,
  data: UpdateFindingStatusRequest,
  auth?: AuthContext,
): Promise<Finding> {
  const f = await apiFetch<FindingApi>(
    `/assessments/${assessmentId}/findings/${findingId}`,
    {
      method: "PATCH",
      body: JSON.stringify({
        status: data.status,
        reviewNote: data.comment,
      }),
      ...authHeaders(auth),
    },
  );
  return mapFinding(f, assessmentId);
}

export async function exportFindingsCsv(
  id: string,
  auth?: AuthContext,
): Promise<{ blob: Blob; filename?: string; contentType: string }> {
  return apiDownload(`/assessments/${id}/findings/export.csv`, auth);
}

export async function getQuestionnaire(
  id: string,
  auth?: AuthContext,
): Promise<Questionnaire> {
  const q = await apiFetch<{
    version: string;
    title: string;
    questions: Array<{
      id: string;
      text: string;
      type: string;
      min?: number;
      max?: number;
      dimension?: string;
    }>;
  }>(`/assessments/${id}/questionnaire`, authHeaders(auth));

  return {
    version: q.version,
    sections: [
      {
        id: "main",
        title: q.title,
        fields: q.questions.map((question) => ({
          id: question.id,
          label: question.text,
          type: "number" as const,
          required: true,
          value: undefined,
          helpText: question.dimension
            ? `Dimensão: ${question.dimension} (${question.min ?? 1}-${question.max ?? 5})`
            : `Faixa ${question.min ?? 1}-${question.max ?? 5}`,
        })),
      },
    ],
  };
}

export async function saveQuestionnaire(
  id: string,
  data: Questionnaire,
  auth?: AuthContext,
): Promise<Questionnaire> {
  const answers: Record<string, number> = {};
  for (const section of data.sections) {
    for (const field of section.fields) {
      const n = Number(field.value);
      if (!Number.isNaN(n)) answers[field.id] = n;
    }
  }
  if (Object.keys(answers).length === 0) {
    throw new Error("Informe ao menos uma resposta numérica válida.");
  }
  await apiFetch(`/assessments/${id}/questionnaire`, {
    method: "POST",
    body: JSON.stringify({ answers }),
    ...authHeaders(auth),
  });
  const refreshed = await getQuestionnaire(id, auth);
  return {
    ...refreshed,
    sections: refreshed.sections.map((section) => ({
      ...section,
      fields: section.fields.map((field) => ({
        ...field,
        value: answers[field.id] ?? field.value,
      })),
    })),
    completedAt: new Date().toISOString(),
  };
}

export async function getDimensions(
  id: string,
  auth?: AuthContext,
): Promise<DimensionScore[]> {
  const dims = await apiFetch<
    Array<{
      dimensionKey: string;
      score: number;
      rationale: string;
      updatedAt: string;
    }>
  >(`/assessments/${id}/dimensions`, authHeaders(auth));

  return dims.map((d, idx) => ({
    id: `${d.dimensionKey}-${idx}`,
    name: d.dimensionKey,
    score: Number(d.score),
    maxScore: 100,
    weight: 1,
    explanation: d.rationale,
    mermaidDiagram: `flowchart LR\n  A[${d.dimensionKey}] --> B[Score ${Number(d.score)}]`,
    factors: [
      {
        name: "diagnóstico",
        impact: Number(d.score),
        description: d.rationale,
      },
    ],
  }));
}

export async function getRoadmap(
  id: string,
  auth?: AuthContext,
): Promise<RoadmapItem[]> {
  const items = await apiFetch<
    Array<{
      id: string;
      title: string;
      description: string;
      phase: string;
      sortOrder: number;
      modernizationOptionId?: string | null;
    }>
  >(`/assessments/${id}/roadmap`, authHeaders(auth));
  return items.map((i) => ({
    id: i.id,
    title: i.title,
    description: i.description,
    phase: normalizePhase(i.phase),
    order: i.sortOrder,
    effort: "MEDIUM",
    status: "PLANNED",
    priority: "MEDIUM",
    modernizationOptionId: i.modernizationOptionId,
  }));
}

export async function saveRoadmap(
  id: string,
  items: RoadmapItem[],
  auth?: AuthContext,
): Promise<RoadmapItem[]> {
  const existing = await getRoadmap(id, auth);
  for (const e of existing) {
    await apiFetch(`/assessments/${id}/roadmap/${e.id}`, {
      method: "DELETE",
      ...authHeaders(auth),
    });
  }
  for (const [idx, item] of items.entries()) {
    if (!item.title.trim()) {
      throw new Error("Todo item do roadmap precisa de título.");
    }
    await apiFetch(`/assessments/${id}/roadmap`, {
      method: "POST",
      body: JSON.stringify({
        title: item.title,
        description: item.description ?? "",
        phase: normalizePhase(item.phase),
        sortOrder: item.order ?? idx,
        modernizationOptionId: item.modernizationOptionId ?? null,
      }),
      ...authHeaders(auth),
    });
  }
  return getRoadmap(id, auth);
}

export async function listReports(
  id: string,
  auth?: AuthContext,
): Promise<ReportSnapshot[]> {
  const list = await apiFetch<
    Array<{
      id: string;
      version: number;
      contentHash: string;
      published: boolean;
      publishedAt?: string;
      createdAt: string;
    }>
  >(`/assessments/${id}/reports`, authHeaders(auth));
  return list.map((snap) => ({
    id: snap.id,
    assessmentId: id,
    version: snap.version,
    contentHash: snap.contentHash,
    published: snap.published,
    status: snap.published ? "PUBLISHED" : "DRAFT",
    publishedAt: snap.publishedAt,
    createdAt: snap.createdAt,
    generatedAt: snap.createdAt,
  }));
}

export async function generateReport(
  id: string,
  auth?: AuthContext,
): Promise<ReportSnapshot> {
  const snap = await apiFetch<{
    id: string;
    version: number;
    contentHash: string;
    published: boolean;
    publishedAt?: string;
    createdAt: string;
  }>(`/assessments/${id}/reports/generate`, {
    method: "POST",
    ...authHeaders(auth),
  });
  return {
    id: snap.id,
    assessmentId: id,
    version: snap.version,
    contentHash: snap.contentHash,
    published: snap.published,
    status: snap.published ? "PUBLISHED" : "DRAFT",
    publishedAt: snap.publishedAt,
    createdAt: snap.createdAt,
    generatedAt: snap.createdAt,
  };
}

export async function publishReport(
  id: string,
  auth?: AuthContext,
  snapshotId?: string,
): Promise<ReportSnapshot> {
  let sid = snapshotId;
  if (!sid) {
    const list = await listReports(id, auth);
    sid = list[0]?.id;
  }
  if (!sid) {
    const generated = await generateReport(id, auth);
    sid = generated.id;
  }
  const snap = await apiFetch<{
    id: string;
    version: number;
    contentHash: string;
    published: boolean;
    publishedAt?: string;
    createdAt: string;
  }>(`/assessments/${id}/reports/${sid}/publish`, {
    method: "POST",
    ...authHeaders(auth),
  });
  return {
    id: snap.id,
    assessmentId: id,
    version: snap.version,
    contentHash: snap.contentHash,
    published: snap.published,
    status: snap.published ? "PUBLISHED" : "DRAFT",
    publishedAt: snap.publishedAt,
    createdAt: snap.createdAt,
    generatedAt: snap.createdAt,
  };
}

export async function downloadReport(
  id: string,
  format: "html" | "pdf",
  auth?: AuthContext,
): Promise<{ blob: Blob; filename?: string; contentType: string }> {
  const list = await listReports(id, auth);
  const sid = list[0]?.id;
  if (!sid) {
    throw new Error("Nenhum relatório gerado");
  }
  return apiDownload(`/assessments/${id}/reports/${sid}/${format}`, auth);
}

export async function getAuditEvents(
  id: string,
  auth?: AuthContext,
): Promise<AuditEvent[]> {
  const events = await apiFetch<
    Array<{
      id: string;
      assessmentId?: string;
      action: string;
      actor: string;
      details: string;
      occurredAt: string;
    }>
  >(`/audit`, authHeaders(auth));
  return events
    .filter((e) => !e.assessmentId || e.assessmentId === id)
    .map((e) => ({
      id: e.id,
      assessmentId: e.assessmentId ?? id,
      action: e.action,
      actor: e.actor,
      details: e.details,
      timestamp: e.occurredAt,
      resourceType: "assessment",
    }));
}

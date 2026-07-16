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
  UpdateFindingStatusRequest,
} from "@/types";

export type AuthContext =
  | string
  | {
      accessToken?: string;
      user?: string;
      roles?: string[];
    }
  | undefined;

function authHeaders(auth?: AuthContext): {
  accessToken?: string;
  headers?: Record<string, string>;
} {
  if (typeof auth === "string") {
    return { accessToken: auth };
  }
  if (auth?.accessToken) {
    return { accessToken: auth.accessToken };
  }
  const user = auth?.user ?? process.env.NEXT_PUBLIC_DEV_USER ?? "architect";
  const roles =
    auth?.roles?.join(",") ??
    process.env.NEXT_PUBLIC_DEV_ROLES ??
    "ARCHITECT,VIEWER,ADMIN";
  return {
    headers: {
      "X-User": user,
      "X-Roles": roles,
    },
  };
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
  const items = await apiFetch<Assessment[]>("/assessments", authHeaders(auth));
  return { items, total: items.length };
}

export async function createAssessment(
  data: CreateAssessmentRequest,
  auth?: AuthContext,
): Promise<Assessment> {
  return apiFetch<Assessment>("/assessments", {
    method: "POST",
    body: JSON.stringify({ name: data.name, description: data.description }),
    ...authHeaders(auth),
  });
}

export async function getAssessment(
  id: string,
  auth?: AuthContext,
): Promise<Assessment> {
  return apiFetch<Assessment>(`/assessments/${id}`, authHeaders(auth));
}

export async function uploadAnalysis(
  id: string,
  file: File,
  auth?: AuthContext,
): Promise<ImportJob> {
  const text = await file.text();
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
  return {
    id: `import-${id}`,
    assessmentId: id,
    status: a.status === "DRAFT" ? "PENDING" : "COMPLETED",
  };
}

export async function getInventory(
  id: string,
  auth?: AuthContext,
): Promise<Inventory> {
  return apiFetch<Inventory>(`/assessments/${id}/inventory`, authHeaders(auth));
}

export async function listFindings(
  id: string,
  params: { status?: string; severity?: string; search?: string } = {},
  auth?: AuthContext,
): Promise<{ items: Finding[]; total: number }> {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.severity) query.set("severity", params.severity);
  const qs = query.toString();
  const raw = await apiFetch<
    Array<{
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
    }>
  >(`/assessments/${id}/findings${qs ? `?${qs}` : ""}`, authHeaders(auth));

  let items: Finding[] = raw.map((f) => ({
    id: f.id,
    assessmentId: id,
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
  }));
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
  const f = await apiFetch<{
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
  }>(`/assessments/${assessmentId}/findings/${findingId}`, {
    method: "PATCH",
    body: JSON.stringify({
      status: data.status,
      reviewNote: data.comment,
    }),
    ...authHeaders(auth),
  });
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

export async function exportFindingsCsv(
  id: string,
  auth?: AuthContext,
): Promise<Blob> {
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
          value: question.min ?? 1,
          helpText: question.dimension
            ? `Dimensão: ${question.dimension} (${question.min ?? 1}-${question.max ?? 5})`
            : undefined,
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
  await apiFetch(`/assessments/${id}/questionnaire`, {
    method: "POST",
    body: JSON.stringify({ answers }),
    ...authHeaders(auth),
  });
  return getQuestionnaire(id, auth);
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
    factors: [{ name: "diagnóstico", impact: Number(d.score), description: d.rationale }],
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
    }>
  >(`/assessments/${id}/roadmap`, authHeaders(auth));
  return items.map((i) => ({
    id: i.id,
    title: i.title,
    description: i.description,
    phase: i.phase,
    order: i.sortOrder,
    effort: "MEDIUM",
    status: "PLANNED",
    priority: "MEDIUM",
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
    await apiFetch(`/assessments/${id}/roadmap`, {
      method: "POST",
      body: JSON.stringify({
        title: item.title,
        description: item.description ?? "",
        phase: String(item.phase ?? "backlog"),
        sortOrder: item.order ?? idx,
        modernizationOptionId: null,
      }),
      ...authHeaders(auth),
    });
  }
  return getRoadmap(id, auth);
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
    const list = await apiFetch<Array<{ id: string }>>(
      `/assessments/${id}/reports`,
      authHeaders(auth),
    );
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
): Promise<Blob> {
  const list = await apiFetch<Array<{ id: string }>>(
    `/assessments/${id}/reports`,
    authHeaders(auth),
  );
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

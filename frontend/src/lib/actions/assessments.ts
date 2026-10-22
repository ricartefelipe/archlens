"use server";

import { getSessionContext } from "@/lib/auth/session";
import {
  createAssessment as apiCreateAssessment,
  saveQuestionnaire as apiSaveQuestionnaire,
  saveRoadmap as apiSaveRoadmap,
  updateFindingStatus as apiUpdateFindingStatus,
  uploadAnalysis as apiUploadAnalysis,
  generateReport as apiGenerateReport,
  publishReport as apiPublishReport,
  downloadReport as apiDownloadReport,
  exportFindingsCsv as apiExportFindingsCsv,
  eraseAssessment as apiEraseAssessment,
} from "@/lib/api/assessments";
import { ApiClientError } from "@/lib/api/client";
import { validateAnalysisFile } from "@/lib/upload/validateAnalysisFile";
import type {
  CreateAssessmentRequest,
  Questionnaire,
  RoadmapItem,
  UpdateFindingStatusRequest,
} from "@/types";
import { redirect } from "next/navigation";

async function sessionAuth() {
  const ctx = await getSessionContext();
  if (!ctx.accessToken && !ctx.userName) {
    throw new ApiClientError({
      status: 401,
      message: "Sessão expirada ou não autenticada. Faça login novamente.",
    });
  }
  return {
    accessToken: ctx.accessToken,
    user: ctx.userName ?? undefined,
    roles: ctx.roles,
  };
}

function rethrow(err: unknown): never {
  if (err instanceof ApiClientError) {
    throw err;
  }
  if (err instanceof Error) {
    throw err;
  }
  throw new Error("Erro inesperado na ação do servidor.");
}

export async function createAssessmentAction(data: CreateAssessmentRequest) {
  try {
    const auth = await sessionAuth();
    if (!data.name?.trim()) {
      throw new Error("Nome da avaliação é obrigatório.");
    }
    return await apiCreateAssessment(data, auth);
  } catch (err) {
    rethrow(err);
  }
}

export async function uploadAnalysisAction(
  assessmentId: string,
  formData: FormData,
) {
  try {
    const file = formData.get("file");
    if (!(file instanceof File)) {
      throw new Error("Arquivo inválido");
    }
    const validation = await validateAnalysisFile(file);
    if (!validation.ok) {
      throw new Error(validation.message);
    }
    const auth = await sessionAuth();
    return await apiUploadAnalysis(
      assessmentId,
      file,
      auth,
      validation.text,
    );
  } catch (err) {
    rethrow(err);
  }
}

export async function updateFindingStatusAction(
  assessmentId: string,
  findingId: string,
  data: UpdateFindingStatusRequest,
) {
  try {
    const auth = await sessionAuth();
    return await apiUpdateFindingStatus(assessmentId, findingId, data, auth);
  } catch (err) {
    rethrow(err);
  }
}

export async function saveQuestionnaireAction(
  assessmentId: string,
  data: Questionnaire,
) {
  try {
    const auth = await sessionAuth();
    return await apiSaveQuestionnaire(assessmentId, data, auth);
  } catch (err) {
    rethrow(err);
  }
}

export async function saveRoadmapAction(
  assessmentId: string,
  items: RoadmapItem[],
) {
  try {
    const auth = await sessionAuth();
    return await apiSaveRoadmap(assessmentId, items, auth);
  } catch (err) {
    rethrow(err);
  }
}

export async function generateReportAction(assessmentId: string) {
  try {
    const auth = await sessionAuth();
    return await apiGenerateReport(assessmentId, auth);
  } catch (err) {
    rethrow(err);
  }
}

export async function publishReportAction(assessmentId: string) {
  try {
    const auth = await sessionAuth();
    return await apiPublishReport(assessmentId, auth);
  } catch (err) {
    rethrow(err);
  }
}

export async function downloadReportAction(
  assessmentId: string,
  format: "html" | "pdf",
): Promise<{ base64: string; contentType: string; filename: string }> {
  try {
    const auth = await sessionAuth();
    const result = await apiDownloadReport(assessmentId, format, auth);
    const buffer = Buffer.from(await result.blob.arrayBuffer());
    if (buffer.byteLength === 0) {
      throw new Error(`Download ${format.toUpperCase()} retornou arquivo vazio.`);
    }
    return {
      base64: buffer.toString("base64"),
      contentType: result.contentType,
      filename:
        result.filename ?? `report-${assessmentId}.${format}`,
    };
  } catch (err) {
    rethrow(err);
  }
}

export async function eraseAssessmentAction(formData: FormData): Promise<void> {
  const assessmentId = String(formData.get("assessmentId") ?? "");
  if (!assessmentId) {
    throw new Error("Avaliação não informada.");
  }
  try {
    const auth = await sessionAuth();
    if (!auth.roles?.includes("ADMIN")) {
      throw new Error("Somente o perfil ADMIN pode retirar os dados da avaliação.");
    }
    await apiEraseAssessment(assessmentId, auth);
  } catch (err) {
    rethrow(err);
  }
  redirect("/assessments");
}

export async function exportFindingsCsvAction(
  assessmentId: string,
): Promise<{ base64: string; contentType: string; filename: string }> {
  try {
    const auth = await sessionAuth();
    const result = await apiExportFindingsCsv(assessmentId, auth);
    const buffer = Buffer.from(await result.blob.arrayBuffer());
    return {
      base64: buffer.toString("base64"),
      contentType: result.contentType,
      filename: result.filename ?? `findings-${assessmentId}.csv`,
    };
  } catch (err) {
    rethrow(err);
  }
}

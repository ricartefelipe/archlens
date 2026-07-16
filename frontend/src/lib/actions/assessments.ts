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
} from "@/lib/api/assessments";
import type {
  CreateAssessmentRequest,
  Questionnaire,
  RoadmapItem,
  UpdateFindingStatusRequest,
} from "@/types";

async function sessionAuth() {
  const ctx = await getSessionContext();
  return {
    accessToken: ctx.accessToken,
    user: ctx.userName ?? "architect",
    roles: ctx.roles,
  };
}

export async function createAssessmentAction(data: CreateAssessmentRequest) {
  const auth = await sessionAuth();
  return apiCreateAssessment(data, auth);
}

export async function uploadAnalysisAction(assessmentId: string, formData: FormData) {
  const file = formData.get("file");
  if (!(file instanceof File)) {
    throw new Error("Arquivo inválido");
  }
  const auth = await sessionAuth();
  return apiUploadAnalysis(assessmentId, file, auth);
}

export async function updateFindingStatusAction(
  assessmentId: string,
  findingId: string,
  data: UpdateFindingStatusRequest,
) {
  const auth = await sessionAuth();
  return apiUpdateFindingStatus(assessmentId, findingId, data, auth);
}

export async function saveQuestionnaireAction(
  assessmentId: string,
  data: Questionnaire,
) {
  const auth = await sessionAuth();
  return apiSaveQuestionnaire(assessmentId, data, auth);
}

export async function saveRoadmapAction(assessmentId: string, items: RoadmapItem[]) {
  const auth = await sessionAuth();
  return apiSaveRoadmap(assessmentId, items, auth);
}

export async function generateReportAction(assessmentId: string) {
  const auth = await sessionAuth();
  return apiGenerateReport(assessmentId, auth);
}

export async function publishReportAction(assessmentId: string) {
  const auth = await sessionAuth();
  return apiPublishReport(assessmentId, auth);
}

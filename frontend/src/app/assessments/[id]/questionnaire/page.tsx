import { AppShell } from "@/components/layout/AppShell";
import { AssessmentNav } from "@/components/layout/AssessmentNav";
import { QuestionnaireForm } from "@/components/assessments/QuestionnaireForm";
import { getQuestionnaire } from "@/lib/api/assessments";
import { getSessionContext } from "@/lib/auth/session";
import { canWrite } from "@/lib/auth/roles";
import type { Questionnaire } from "@/types";

export const metadata = { title: "Questionário" };

const emptyQuestionnaire: Questionnaire = {
  version: "1.0.0",
  sections: [
    {
      id: "context",
      title: "Contexto do sistema",
      description: "Informações complementares à análise estática.",
      fields: [
        {
          id: "business_criticality",
          label: "Criticidade de negócio",
          type: "select",
          required: true,
          options: ["Baixa", "Média", "Alta", "Crítica"],
        },
        {
          id: "deployment_model",
          label: "Modelo de deploy",
          type: "select",
          required: true,
          options: ["On-premises", "Cloud", "Híbrido"],
        },
        {
          id: "modernization_goal",
          label: "Objetivo de modernização",
          type: "textarea",
          required: true,
          helpText: "Descreva metas em 12–24 meses.",
        },
        {
          id: "team_size",
          label: "Tamanho do time (devs)",
          type: "number",
          required: false,
        },
        {
          id: "has_test_automation",
          label: "Possui automação de testes",
          type: "boolean",
          required: false,
        },
      ],
    },
  ],
};

export default async function QuestionnairePage({
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
  let questionnaire = emptyQuestionnaire;

  try {
    questionnaire = await getQuestionnaire(id, auth);
  } catch (err) {
    error =
      err instanceof Error
        ? "Backend indisponível — exibindo formulário padrão local."
        : "Erro ao carregar questionário.";
  }

  return (
    <AppShell>
      <AssessmentNav assessmentId={id} />
      <h1 className="mb-6 text-2xl font-bold text-slate-900">Questionário</h1>
      {error ? (
        <p className="mb-4 text-sm text-amber-700" role="status">
          {error}
        </p>
      ) : null}
      <QuestionnaireForm
        assessmentId={id}
        initialData={questionnaire}
        canEdit={canWrite(roles)}
      />
    </AppShell>
  );
}

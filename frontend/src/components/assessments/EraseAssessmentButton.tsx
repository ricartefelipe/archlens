"use client";

import { Button } from "@/components/ui/Button";
import { eraseAssessmentAction } from "@/lib/actions/assessments";

export function EraseAssessmentButton({ assessmentId }: { assessmentId: string }) {
  return (
    <form action={eraseAssessmentAction}>
      <input type="hidden" name="assessmentId" value={assessmentId} />
      <Button
        type="submit"
        variant="danger"
        onClick={(event) => {
          const confirmed = window.confirm(
            "Remove inventário, achados, questionário, roadmap e relatórios desta avaliação. A trilha de auditoria permanece sem o conteúdo da análise.",
          );
          if (!confirmed) {
            event.preventDefault();
          }
        }}
      >
        Retirar dados da avaliação
      </Button>
    </form>
  );
}

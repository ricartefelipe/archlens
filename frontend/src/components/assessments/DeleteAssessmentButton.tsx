"use client";

import { useState, useTransition } from "react";
import { deleteAssessmentAction } from "@/lib/actions/assessments";
import { Button } from "@/components/ui/Button";

function isNextRedirect(err: unknown): boolean {
  if (typeof err !== "object" || err === null || !("digest" in err)) {
    return false;
  }
  const digest = (err as { digest?: unknown }).digest;
  return typeof digest === "string" && digest.startsWith("NEXT_REDIRECT");
}

export function DeleteAssessmentButton({ assessmentId }: { assessmentId: string }) {
  const [error, setError] = useState<string | null>(null);
  const [pending, startTransition] = useTransition();

  return (
    <div className="space-y-2">
      <Button
        type="button"
        variant="danger"
        disabled={pending}
        aria-busy={pending}
        onClick={() => {
          const confirmed = window.confirm(
            "Excluir esta avaliação, os achados e os relatórios? O download deixa de existir. A trilha de auditoria permanece se a retenção estiver ativa.",
          );
          if (!confirmed) {
            return;
          }
          setError(null);
          startTransition(async () => {
            try {
              await deleteAssessmentAction(assessmentId);
            } catch (err) {
              if (isNextRedirect(err)) {
                throw err;
              }
              setError(err instanceof Error ? err.message : "Falha ao excluir a avaliação.");
            }
          });
        }}
      >
        {pending ? "Excluindo…" : "Excluir avaliação"}
      </Button>
      {error ? (
        <p role="alert" className="text-sm text-red-700">
          {error}
        </p>
      ) : null}
    </div>
  );
}

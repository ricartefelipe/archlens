"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { saveQuestionnaireAction } from "@/lib/actions/assessments";
import type { Questionnaire, QuestionnaireField } from "@/types";

function updateFieldValue(
  questionnaire: Questionnaire,
  sectionId: string,
  fieldId: string,
  value: string | number | boolean,
): Questionnaire {
  return {
    ...questionnaire,
    sections: questionnaire.sections.map((section) =>
      section.id === sectionId
        ? {
            ...section,
            fields: section.fields.map((field) =>
              field.id === fieldId ? { ...field, value } : field,
            ),
          }
        : section,
    ),
  };
}

export function QuestionnaireForm({
  assessmentId,
  initialData,
  canEdit,
}: {
  assessmentId: string;
  initialData: Questionnaire;
  canEdit: boolean;
}) {
  const [questionnaire, setQuestionnaire] = useState(initialData);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  function renderField(
    sectionId: string,
    field: QuestionnaireField,
  ): React.ReactNode {
    const commonProps = {
      id: field.id,
      name: field.id,
      required: field.required,
      disabled: !canEdit,
      value: field.value?.toString() ?? "",
      onChange: (
        e: React.ChangeEvent<
          HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
        >,
      ) => {
        const value =
          field.type === "number"
            ? Number(e.target.value)
            : field.type === "boolean"
              ? (e.target as HTMLInputElement).checked
              : e.target.value;
        setQuestionnaire((prev) =>
          updateFieldValue(prev, sectionId, field.id, value),
        );
        setSaved(false);
      },
    };

    switch (field.type) {
      case "textarea":
        return (
          <textarea
            {...commonProps}
            rows={4}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        );
      case "select":
        return (
          <select
            {...commonProps}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            <option value="">Selecione…</option>
            {field.options?.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
        );
      case "boolean":
        return (
          <input
            type="checkbox"
            id={field.id}
            name={field.id}
            checked={Boolean(field.value)}
            disabled={!canEdit}
            onChange={commonProps.onChange}
            className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600"
          />
        );
      case "number":
        return (
          <input
            type="number"
            {...commonProps}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        );
      case "text":
        return (
          <input
            type="text"
            {...commonProps}
            className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        );
      default: {
        const _exhaustive: never = field.type;
        return _exhaustive;
      }
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!canEdit) {
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const savedData = await saveQuestionnaireAction(assessmentId, questionnaire);
      setQuestionnaire(savedData);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao salvar questionário");
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <p className="text-sm text-slate-600">
        Versão do formulário: <strong>{questionnaire.version}</strong>
      </p>

      {questionnaire.sections.map((section) => (
        <Card key={section.id}>
          <CardHeader title={section.title} description={section.description} />
          <CardBody className="space-y-4">
            {section.fields.map((field) => (
              <div key={field.id}>
                <label htmlFor={field.id} className="block text-sm font-medium">
                  {field.label}
                  {field.required ? " *" : ""}
                </label>
                {field.helpText ? (
                  <p className="mt-1 text-xs text-slate-500">{field.helpText}</p>
                ) : null}
                {renderField(section.id, field)}
              </div>
            ))}
          </CardBody>
        </Card>
      ))}

      {error ? <ErrorState message={error} /> : null}
      {saved ? (
        <p className="text-sm text-green-700" role="status">
          Questionário salvo com sucesso.
        </p>
      ) : null}

      {canEdit ? (
        <Button type="submit" disabled={saving}>
          {saving ? "Salvando…" : "Salvar questionário"}
        </Button>
      ) : (
        <p className="text-sm text-slate-500">
          Modo somente leitura (VIEWER).
        </p>
      )}
    </form>
  );
}

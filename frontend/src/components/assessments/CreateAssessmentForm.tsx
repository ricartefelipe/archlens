"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { createAssessmentAction } from "@/lib/actions/assessments";

export function CreateAssessmentForm() {
  const router = useRouter();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [targetProjectName, setTargetProjectName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (loading) return;
    if (!name.trim()) {
      setError("Nome da avaliação é obrigatório.");
      return;
    }
    setLoading(true);
    setError(null);

    try {
      const assessment = await createAssessmentAction({
        name: name.trim(),
        description: description.trim() || undefined,
        targetProjectName: targetProjectName.trim() || undefined,
      });
      router.push(`/assessments/${assessment.id}`);
      router.refresh();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Não foi possível criar a avaliação. Verifique se o backend está disponível.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <Card>
      <CardHeader
        title="Nova avaliação"
        description="Inicie um diagnóstico arquitetural para um projeto Java."
      />
      <CardBody>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-slate-700">
              Nome *
            </label>
            <input
              id="name"
              name="name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
          <div>
            <label
              htmlFor="targetProjectName"
              className="block text-sm font-medium text-slate-700"
            >
              Projeto alvo
            </label>
            <input
              id="targetProjectName"
              name="targetProjectName"
              value={targetProjectName}
              onChange={(e) => setTargetProjectName(e.target.value)}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
          <div>
            <label
              htmlFor="description"
              className="block text-sm font-medium text-slate-700"
            >
              Descrição
            </label>
            <textarea
              id="description"
              name="description"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            />
          </div>
          {error ? (
            <p className="text-sm text-red-700" role="alert">
              {error}
            </p>
          ) : null}
          <Button type="submit" disabled={loading}>
            {loading ? "Criando…" : "Criar avaliação"}
          </Button>
        </form>
      </CardBody>
    </Card>
  );
}

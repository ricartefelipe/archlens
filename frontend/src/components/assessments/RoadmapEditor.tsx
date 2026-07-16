"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Card, CardBody, CardHeader } from "@/components/ui/Card";
import { ErrorState } from "@/components/ui/ErrorState";
import { saveRoadmapAction } from "@/lib/actions/assessments";
import type { RoadmapItem } from "@/types";

function emptyItem(): RoadmapItem {
  return {
    id: crypto.randomUUID(),
    title: "",
    description: "",
    priority: "MEDIUM",
    effort: "MEDIUM",
    phase: 1,
    status: "PLANNED",
  };
}

export function RoadmapEditor({
  assessmentId,
  initialItems,
  canEdit,
}: {
  assessmentId: string;
  initialItems: RoadmapItem[];
  canEdit: boolean;
}) {
  const [items, setItems] = useState(initialItems);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  function updateItem(id: string, patch: Partial<RoadmapItem>) {
    setItems((prev) =>
      prev.map((item) => (item.id === id ? { ...item, ...patch } : item)),
    );
    setSaved(false);
  }

  async function handleSave() {
    setSaving(true);
    setError(null);
    try {
      const savedItems = await saveRoadmapAction(assessmentId, items);
      setItems(savedItems);
      setSaved(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Erro ao salvar roadmap");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-4">
      {items.length === 0 ? (
        <p className="text-sm text-slate-600">
          Nenhum item de roadmap. {canEdit ? "Adicione opções de modernização abaixo." : ""}
        </p>
      ) : null}

      {items.map((item) => (
        <Card key={item.id}>
          <CardHeader
            title={item.title || "Novo item"}
            action={<Badge label={item.priority ?? "MEDIUM"} />}
          />
          <CardBody className="space-y-3">
            <div>
              <label className="block text-sm font-medium" htmlFor={`title-${item.id}`}>
                Título
              </label>
              <input
                id={`title-${item.id}`}
                value={item.title}
                disabled={!canEdit}
                onChange={(e) => updateItem(item.id, { title: e.target.value })}
                className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label
                className="block text-sm font-medium"
                htmlFor={`desc-${item.id}`}
              >
                Descrição
              </label>
              <textarea
                id={`desc-${item.id}`}
                value={item.description}
                disabled={!canEdit}
                rows={3}
                onChange={(e) =>
                  updateItem(item.id, { description: e.target.value })
                }
                className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <div>
                <label className="block text-sm font-medium" htmlFor={`phase-${item.id}`}>
                  Fase
                </label>
                <input
                  id={`phase-${item.id}`}
                  type="number"
                  min={1}
                  disabled={!canEdit}
                  value={item.phase}
                  onChange={(e) =>
                    updateItem(item.id, { phase: Number(e.target.value) })
                  }
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label className="block text-sm font-medium" htmlFor={`effort-${item.id}`}>
                  Esforço
                </label>
                <select
                  id={`effort-${item.id}`}
                  disabled={!canEdit}
                  value={item.effort}
                  onChange={(e) =>
                    updateItem(item.id, {
                      effort: e.target.value as RoadmapItem["effort"],
                    })
                  }
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="SMALL">SMALL</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="LARGE">LARGE</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium" htmlFor={`status-${item.id}`}>
                  Status
                </label>
                <select
                  id={`status-${item.id}`}
                  disabled={!canEdit}
                  value={item.status}
                  onChange={(e) =>
                    updateItem(item.id, {
                      status: e.target.value as RoadmapItem["status"],
                    })
                  }
                  className="mt-1 w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                >
                  <option value="PLANNED">PLANNED</option>
                  <option value="IN_PROGRESS">IN_PROGRESS</option>
                  <option value="DONE">DONE</option>
                </select>
              </div>
            </div>
          </CardBody>
        </Card>
      ))}

      {error ? <ErrorState message={error} /> : null}
      {saved ? (
        <p className="text-sm text-green-700" role="status">
          Roadmap salvo.
        </p>
      ) : null}

      {canEdit ? (
        <div className="flex flex-wrap gap-3">
          <Button variant="secondary" onClick={() => setItems((prev) => [...prev, emptyItem()])}>
            Adicionar item
          </Button>
          <Button onClick={() => void handleSave()} disabled={saving}>
            {saving ? "Salvando…" : "Salvar roadmap"}
          </Button>
        </div>
      ) : (
        <p className="text-sm text-slate-500">Modo somente leitura (VIEWER).</p>
      )}
    </div>
  );
}

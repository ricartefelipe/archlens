"use client";

import { useEffect, useRef, useState } from "react";

export function MermaidDiagram({
  chart,
  fallbackText,
}: {
  chart: string;
  fallbackText?: string;
}) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [error, setError] = useState<string | null>(null);
  const [rendered, setRendered] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function renderChart() {
      if (!containerRef.current || !chart.trim()) {
        return;
      }

      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({
          startOnLoad: false,
          theme: "neutral",
          securityLevel: "strict",
        });

        const id = `mermaid-${crypto.randomUUID().replace(/-/g, "")}`;
        const { svg } = await mermaid.render(id, chart);

        if (!cancelled && containerRef.current) {
          containerRef.current.innerHTML = svg;
          setRendered(true);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? err.message : "Falha ao renderizar diagrama",
          );
          setRendered(false);
        }
      }
    }

    void renderChart();

    return () => {
      cancelled = true;
    };
  }, [chart]);

  if (!chart.trim()) {
    return (
      <pre className="overflow-x-auto rounded-md bg-slate-50 p-4 text-sm text-slate-700">
        {fallbackText ?? "Diagrama indisponível."}
      </pre>
    );
  }

  return (
    <div>
      <div
        ref={containerRef}
        className="overflow-x-auto rounded-md border border-slate-200 bg-white p-4"
        role="img"
        aria-label="Diagrama arquitetural"
      />
      {!rendered || error ? (
        <details className="mt-3 rounded-md border border-slate-200 bg-slate-50 p-3">
          <summary className="cursor-pointer text-sm font-medium text-slate-700">
            {error ? "Ver representação textual (fallback)" : "Carregando diagrama…"}
          </summary>
          <pre className="mt-2 overflow-x-auto whitespace-pre-wrap text-xs text-slate-600">
            {fallbackText ?? chart}
          </pre>
          {error ? (
            <p className="mt-2 text-xs text-amber-700" role="status">
              Mermaid: {error}
            </p>
          ) : null}
        </details>
      ) : null}
    </div>
  );
}

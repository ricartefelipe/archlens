"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";

const journeySteps = [
  { href: "", label: "Visão geral", suffix: "" },
  { href: "/import", label: "Importar scan", suffix: "/import" },
  { href: "/inventory", label: "Inventário", suffix: "/inventory" },
  { href: "/findings", label: "Achados", suffix: "/findings" },
  { href: "/questionnaire", label: "Questionário", suffix: "/questionnaire" },
  { href: "/dimensions", label: "Dimensões", suffix: "/dimensions" },
  { href: "/roadmap", label: "Roadmap", suffix: "/roadmap" },
  { href: "/report", label: "Relatório", suffix: "/report" },
  { href: "/audit", label: "Auditoria", suffix: "/audit" },
] as const;

export function AssessmentNav({ assessmentId }: { assessmentId: string }) {
  const pathname = usePathname();
  const base = `/assessments/${assessmentId}`;

  return (
    <nav aria-label="Jornada da avaliação" className="mb-6">
      <ol className="flex flex-wrap gap-2">
        {journeySteps.map((step) => {
          const href = `${base}${step.suffix}`;
          const isActive =
            step.suffix === ""
              ? pathname === base
              : pathname.startsWith(href);

          return (
            <li key={step.suffix}>
              <Link
                href={href}
                className={cn(
                  "inline-flex rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500",
                  isActive
                    ? "bg-brand-600 text-white"
                    : "bg-slate-100 text-slate-700 hover:bg-slate-200",
                )}
                aria-current={isActive ? "page" : undefined}
              >
                {step.label}
              </Link>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

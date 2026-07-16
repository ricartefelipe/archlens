import type { Finding } from "@/types";

function escapeCsvValue(value: string): string {
  if (value.includes(",") || value.includes('"') || value.includes("\n")) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

export function findingsToCsv(findings: Finding[]): string {
  const headers = [
    "id",
    "ruleId",
    "title",
    "severity",
    "status",
    "category",
    "description",
    "recommendation",
    "updatedAt",
  ];

  const rows = findings.map((finding) =>
    [
      finding.id,
      finding.ruleId,
      finding.title,
      finding.severity,
      finding.status,
      finding.category,
      finding.description,
      finding.recommendation ?? "",
      finding.updatedAt,
    ]
      .map(String)
      .map(escapeCsvValue)
      .join(","),
  );

  return [headers.join(","), ...rows].join("\n");
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function formatDate(iso: string): string {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(iso));
}

export function formatStatusLabel(status: string): string {
  return status
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(" ");
}

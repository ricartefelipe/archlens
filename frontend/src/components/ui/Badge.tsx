import { cn, formatStatusLabel } from "@/lib/utils";

const statusColors: Record<string, string> = {
  DRAFT: "bg-slate-100 text-slate-700",
  IMPORT_PENDING: "bg-amber-100 text-amber-800",
  IMPORTING: "bg-amber-100 text-amber-800",
  IMPORTED: "bg-blue-100 text-blue-800",
  ANALYZING: "bg-blue-100 text-blue-800",
  READY: "bg-green-100 text-green-800",
  PUBLISHED: "bg-brand-100 text-brand-800",
  OPEN: "bg-amber-100 text-amber-800",
  APPROVED: "bg-green-100 text-green-800",
  EDITED: "bg-blue-100 text-blue-800",
  RISK_ACCEPTED: "bg-purple-100 text-purple-800",
  DISMISSED: "bg-slate-100 text-slate-700",
  CRITICAL: "bg-red-100 text-red-800",
  HIGH: "bg-orange-100 text-orange-800",
  MEDIUM: "bg-amber-100 text-amber-800",
  LOW: "bg-blue-100 text-blue-800",
  INFO: "bg-slate-100 text-slate-700",
};

export function Badge({
  label,
  className,
}: {
  label: string;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium",
        statusColors[label] ?? "bg-slate-100 text-slate-700",
        className,
      )}
    >
      {formatStatusLabel(label)}
    </span>
  );
}

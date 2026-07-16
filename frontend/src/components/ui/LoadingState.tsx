export function LoadingState({ label = "Carregando…" }: { label?: string }) {
  return (
    <div
      className="flex items-center gap-3 rounded-lg border border-slate-200 bg-slate-50 px-4 py-8 text-slate-600"
      role="status"
      aria-live="polite"
    >
      <span
        className="inline-block h-5 w-5 animate-spin rounded-full border-2 border-slate-300 border-t-brand-600"
        aria-hidden="true"
      />
      <span>{label}</span>
    </div>
  );
}

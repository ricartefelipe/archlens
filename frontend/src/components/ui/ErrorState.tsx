import { Button } from "@/components/ui/Button";

export function ErrorState({
  title = "Não foi possível carregar os dados",
  message,
  onRetry,
}: {
  title?: string;
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div
      className="rounded-lg border border-red-200 bg-red-50 px-4 py-6 text-red-900"
      role="alert"
    >
      <h3 className="font-semibold">{title}</h3>
      {message ? <p className="mt-2 text-sm">{message}</p> : null}
      {onRetry ? (
        <div className="mt-4">
          <Button variant="secondary" size="sm" onClick={onRetry}>
            Tentar novamente
          </Button>
        </div>
      ) : null}
    </div>
  );
}

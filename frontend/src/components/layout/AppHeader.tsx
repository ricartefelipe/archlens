import Link from "next/link";
import { signOut } from "@/auth";
import { Button } from "@/components/ui/Button";
import type { UserRole } from "@/types";

export function AppHeader({
  userName,
  roles,
}: {
  userName?: string | null;
  roles: UserRole[];
}) {
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-8">
          <Link
            href="/"
            className="flex items-center gap-2 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
          >
            <span
              className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-sm font-bold text-white"
              aria-hidden="true"
            >
              AL
            </span>
            <span className="text-lg font-semibold text-slate-900">ArchLens</span>
          </Link>
          <nav aria-label="Principal" className="hidden gap-4 sm:flex">
            <Link
              href="/"
              className="text-sm font-medium text-slate-600 hover:text-brand-700"
            >
              Início
            </Link>
            <Link
              href="/assessments"
              className="text-sm font-medium text-slate-600 hover:text-brand-700"
            >
              Avaliações
            </Link>
          </nav>
        </div>
        <div className="flex items-center gap-4">
          {userName ? (
            <div className="text-right text-sm">
              <p className="font-medium text-slate-900">{userName}</p>
              <p className="text-slate-500">{roles.join(", ")}</p>
            </div>
          ) : null}
          <form
            action={async () => {
              "use server";
              await signOut({ redirectTo: "/login" });
            }}
          >
            <Button type="submit" variant="ghost" size="sm">
              Sair
            </Button>
          </form>
        </div>
      </div>
    </header>
  );
}

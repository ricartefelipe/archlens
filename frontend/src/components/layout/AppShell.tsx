import { AppHeader } from "@/components/layout/AppHeader";
import { getSessionContext } from "@/lib/auth/session";

export async function AppShell({ children }: { children: React.ReactNode }) {
  const { roles, userName } = await getSessionContext();

  return (
    <div className="min-h-screen bg-slate-50">
      <AppHeader userName={userName} roles={roles} />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">{children}</main>
    </div>
  );
}

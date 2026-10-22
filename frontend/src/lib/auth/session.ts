import { auth } from "@/auth";
import type { UserRole } from "@/types";

export async function getSessionContext(): Promise<{
  accessToken?: string;
  roles: UserRole[];
  userName?: string | null;
  userEmail?: string | null;
}> {
  const session = await auth();
  const email = session?.user?.email ?? undefined;
  const userName =
    email?.split("@")[0] ??
    session?.user?.name ??
    undefined;
  return {
    accessToken: session?.accessToken,
    roles: session?.roles ?? ["VIEWER"],
    // Nunca rebaixar uma sessão OIDC para identidade por headers locais.
    // Só sessões provenientes do provedor Credentials local podem usar X-User.
    userName: session?.authProvider === "local" ? userName : undefined,
    userEmail: session?.user?.email,
  };
}

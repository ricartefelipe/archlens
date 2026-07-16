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
    userName,
    userEmail: session?.user?.email,
  };
}

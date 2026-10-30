import { auth } from "@/auth";
import { NextResponse } from "next/server";
import { isSessionUsable, SESSION_COOKIE_NAMES } from "@/lib/auth/sessionState";

const publicPaths = ["/login", "/api/auth"];

export default auth((req) => {
  const { pathname } = req.nextUrl;
  const isPublic = publicPaths.some(
    (path) => pathname === path || pathname.startsWith(`${path}/`),
  );

  if (isPublic) {
    return NextResponse.next();
  }

  if (!isSessionUsable(req.auth)) {
    if (pathname === "/" && !req.auth) {
      return NextResponse.next();
    }
    const loginUrl = new URL("/login", req.nextUrl.origin);
    loginUrl.searchParams.set("callbackUrl", pathname);
    const response = NextResponse.redirect(loginUrl);
    if (req.auth) {
      // A renovação falhou (sessão revogada ou expirada no IdP): descarta o cookie para que a
      // sessão inválida não sobreviva ao redirecionamento.
      for (const name of SESSION_COOKIE_NAMES) {
        response.cookies.delete(name);
      }
    }
    return response;
  }

  return NextResponse.next();
});

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"],
};

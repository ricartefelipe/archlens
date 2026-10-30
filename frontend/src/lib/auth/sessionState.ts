/**
 * Uma sessão só serve para navegar quando ainda tem como falar com a API: depois de uma falha de
 * renovação o access token foi descartado e manter o usuário "logado" deixaria páginas vazias e
 * chamadas 401. Nesse caso a sessão deve ser encerrada e o login refeito (falha fechada).
 */
export function isSessionUsable(session: { error?: string } | null | undefined): boolean {
  return Boolean(session) && !session?.error;
}

/** Nomes do cookie de sessão do Auth.js em HTTP e em HTTPS. */
export const SESSION_COOKIE_NAMES = ["authjs.session-token", "__Secure-authjs.session-token"] as const;

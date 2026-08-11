import "server-only";
import { cache } from "react";
import { headers } from "next/headers";
import { getToken, type JWT } from "next-auth/jwt";
import { precisaRenovar, renovarToken } from "@/lib/auth-token-refresh";

/**
 * Lê o JWT (cookie httpOnly) direto, sem passar pelo endpoint público
 * /api/auth/session — accessToken/idToken nunca são expostos ali de
 * propósito (ver auth.ts, callback session). secureCookie replica a
 * regra do próprio Auth.js (useSecureCookies = protocolo https) — dev
 * é sempre http, produção sempre https via Cloudflare Tunnel
 * (ADR-0019), então NODE_ENV é um proxy confiável pros dois ambientes
 * reais deste projeto.
 */
async function obterTokenBruto(): Promise<JWT | null> {
  return getToken({
    req: { headers: await headers() } as never,
    secret: process.env.AUTH_SECRET,
    secureCookie: process.env.NODE_ENV === "production",
  });
}

/**
 * Achado real (2026-08-10): getToken() só DECODIFICA o cookie — nunca
 * passa pelo callbacks.jwt do auth.ts, que é onde a renovação
 * automática está implementada. Isso só acontecia quando algo chamava
 * auth() (ex: o layout, pra checar sessão). Resultado: qualquer chamada
 * a um microsserviço que acontecesse mais de 5min (vida do access
 * token do Keycloak) depois do login/última renovação usava um token
 * expirado e tomava 401 — reproduzido de verdade tentando anexar uma
 * fatura (document-service) e no dashboard (transaction-service).
 * Corrigido fazendo o próprio obterAccessToken() checar e renovar.
 *
 * cache() do React memoiza por request (via AsyncLocalStorage, seguro
 * pra dado multi-tenant — nunca vaza entre requests/usuários) — sem
 * isso, o dashboard chamaria renovarToken() até 6 vezes em paralelo
 * (Promise.all de account/transaction/budget-service), todas com o
 * mesmo refresh_token ainda não trocado, desperdiçando chamada ao
 * Keycloak à toa.
 */
const obterTokenAtualizado = cache(async (): Promise<JWT | null> => {
  const token = await obterTokenBruto();
  if (!token || !precisaRenovar(token.expiresAt) || !token.refreshToken) {
    return token;
  }

  try {
    const renovado = await renovarToken(
      token.refreshToken,
      process.env.AUTH_KEYCLOAK_ISSUER!,
      process.env.AUTH_KEYCLOAK_ID!
    );
    return {
      ...token,
      accessToken: renovado.accessToken,
      refreshToken: renovado.refreshToken,
      idToken: renovado.idToken,
      expiresAt: renovado.expiresAt,
    };
  } catch {
    // Falha ao renovar (ex: refresh_token também expirou) — devolve o
    // token velho mesmo; a chamada ao microsserviço vai dar 401 de
    // verdade, o que é correto (sessão realmente inválida, precisa
    // logar de novo — não é um caso pra mascarar).
    return token;
  }
});

/** Pra propagar `Authorization: Bearer` nas chamadas aos microsserviços. */
export async function obterAccessToken(): Promise<string | null> {
  const token = await obterTokenAtualizado();
  return token?.accessToken ?? null;
}

/** Só usado no logout (id_token_hint do RP-Initiated Logout), ver
 * lib/auth-actions.ts — não precisa do token renovado, o cru já serve. */
export async function obterIdToken(): Promise<string | null> {
  const token = await obterTokenBruto();
  return token?.idToken ?? null;
}

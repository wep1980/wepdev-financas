import "server-only";
import { cache } from "react";
import { headers } from "next/headers";
import { getToken, type JWT } from "next-auth/jwt";
import { precisaRenovar, renovarToken } from "@/lib/auth-token-refresh";

/**
 * Lê o JWT (cookie httpOnly) direto, sem passar pelo endpoint público
 * /api/auth/session — accessToken/idToken nunca são expostos ali de
 * propósito (ver auth.ts, callback session).
 *
 * `secureCookie` decide o NOME do cookie que getToken() procura
 * (`__Secure-authjs.session-token` vs `authjs.session-token` sem
 * prefixo) — precisa bater exatamente com o que o próprio Auth.js usou
 * pra SETAR o cookie, que é decidido pelo protocolo real da requisição
 * (`url.protocol === "https:"`, ver @auth/core/src/lib/init.ts), nunca
 * por `NODE_ENV`. Achado real rodando o container de produção pela
 * primeira vez (fatia 6, fechamento — 2026-08-11): a imagem Docker tem
 * `NODE_ENV=production` fixo (Dockerfile), mas em dev local via
 * docker-compose o protocolo continua http (só produção real, atrás do
 * Cloudflare Tunnel — ADR-0019 — é https); usar `NODE_ENV` pra decidir
 * o nome do cookie fazia essa função nunca achar o cookie certo dentro
 * do container, devolvendo `null` sempre, apesar da sessão existir de
 * verdade (confirmado comparando com `auth()`, que decodifica o mesmo
 * cookie corretamente porque usa o protocolo real, não `NODE_ENV`).
 * Corrigido tentando os dois nomes em vez de adivinhar por variável de
 * ambiente — funciona em dev, atrás do Cloudflare Tunnel e em qualquer
 * outra topologia futura, sem depender de header de proxy específico.
 */
async function obterTokenBruto(): Promise<JWT | null> {
  const params = { req: { headers: await headers() } as never, secret: process.env.AUTH_SECRET };
  return (
    (await getToken({ ...params, secureCookie: true })) ??
    (await getToken({ ...params, secureCookie: false }))
  );
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
      // Chamada de servidor — precisa do endpoint interno (alcançável de
      // dentro do container), não do issuer público do navegador. Ver
      // auth.ts.
      process.env.AUTH_KEYCLOAK_INTERNAL_ISSUER ?? process.env.AUTH_KEYCLOAK_ISSUER!,
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

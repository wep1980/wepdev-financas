export interface TokenSet {
  accessToken: string;
  refreshToken?: string;
  idToken?: string;
  expiresAt: number;
}

/** Extraída do callback jwt do Auth.js pra ficar testável sem subir o NextAuth inteiro. */
export function precisaRenovar(
  expiresAt: number | undefined,
  agoraEmSegundos: number = Date.now() / 1000
): boolean {
  if (expiresAt === undefined) return true;
  return agoraEmSegundos >= expiresAt;
}

/**
 * Troca o refresh_token por um access_token novo direto no endpoint de
 * token do Keycloak (grant_type=refresh_token) — mesmo client público
 * (web-app, sem client_secret) usado no login.
 */
export async function renovarToken(
  refreshToken: string,
  issuer: string,
  clientId: string,
  fetchImpl: typeof fetch = fetch
): Promise<TokenSet> {
  const resposta = await fetchImpl(`${issuer}/protocol/openid-connect/token`, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "refresh_token",
      client_id: clientId,
      refresh_token: refreshToken,
    }),
  });

  if (!resposta.ok) {
    throw new Error(
      `Falha ao renovar token de acesso do Keycloak: ${resposta.status}`
    );
  }

  const dados = await resposta.json();
  return {
    accessToken: dados.access_token,
    refreshToken: dados.refresh_token ?? refreshToken,
    idToken: dados.id_token,
    expiresAt: Math.floor(Date.now() / 1000) + dados.expires_in,
  };
}

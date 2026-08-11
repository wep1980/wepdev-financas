"use server";

import { redirect } from "next/navigation";
import { signIn, signOut } from "@/auth";
import { obterIdToken } from "@/lib/auth-token";

export async function entrar() {
  await signIn("keycloak");
}

/**
 * signOut() do Auth.js só limpa a sessão local (cookie) — sem isso o
 * usuário continua "logado" do lado do Keycloak (SSO), e um login
 * seguinte não pediria senha de novo. ADR-0027 pede logout completo:
 * limpa local E encerra a sessão no Keycloak via RP-Initiated Logout
 * (id_token_hint identifica o client, dispensa client_secret — client
 * público, ver auth.ts).
 */
export async function sair() {
  const idToken = await obterIdToken();

  await signOut({ redirect: false });

  const issuer = process.env.AUTH_KEYCLOAK_ISSUER!;
  const params = new URLSearchParams({
    post_logout_redirect_uri: process.env.AUTH_URL ?? "http://localhost:3000",
  });
  if (idToken) {
    params.set("id_token_hint", idToken);
  } else {
    params.set("client_id", process.env.AUTH_KEYCLOAK_ID!);
  }

  redirect(`${issuer}/protocol/openid-connect/logout?${params}`);
}

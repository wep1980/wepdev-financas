import type { DefaultSession } from "next-auth";
import type { DefaultJWT } from "next-auth/jwt";

// sub, accessToken etc. armazenados no JWT (cookie httpOnly) — accessToken
// NUNCA entra em Session (ver módulo augmentation abaixo), só é lido
// server-side via getToken() em lib/auth-token.ts (ADR-0027).
declare module "next-auth/jwt" {
  interface JWT extends DefaultJWT {
    accessToken?: string;
    refreshToken?: string;
    idToken?: string;
    expiresAt?: number;
    error?: "FalhaAoRenovarToken" | "SemRefreshToken";
  }
}

declare module "next-auth" {
  interface Session extends DefaultSession {
    user: {
      id: string;
    } & DefaultSession["user"];
    error?: "FalhaAoRenovarToken" | "SemRefreshToken";
  }
}

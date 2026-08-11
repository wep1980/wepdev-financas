import NextAuth from "next-auth";
import Keycloak from "next-auth/providers/keycloak";
import { precisaRenovar, renovarToken } from "@/lib/auth-token-refresh";

const issuer = process.env.AUTH_KEYCLOAK_ISSUER!;
// Endpoint de token/userinfo/jwks é chamado pelo próprio servidor Next.js —
// dentro do docker-compose isso roda num container, que não alcança
// "localhost:8080" (é o próprio container, não o host, ver KC_HOSTNAME no
// docker-compose.yml). "issuer" acima continua sendo o valor público
// (alcançável só pelo navegador, usado no redirect de login/logout e como
// valor esperado do claim "iss" do token — KC_HOSTNAME=localhost fixa o
// "iss" como público não importa quem pediu, mesmo achado já resolvido nos
// serviços Java). Sem AUTH_KEYCLOAK_INTERNAL_ISSUER (dev local, navegador e
// servidor no mesmo host) os dois valores são iguais.
const issuerInterno = process.env.AUTH_KEYCLOAK_INTERNAL_ISSUER ?? issuer;
const clientId = process.env.AUTH_KEYCLOAK_ID!;

export const { handlers, auth, signIn, signOut } = NextAuth({
  // Self-hosted (ADR-0016), não Vercel — sem isso o Auth.js rejeita o Host
  // header do request com "UntrustedHost".
  trustHost: true,
  providers: [
    Keycloak({
      clientId,
      issuer,
      // Client "web-app" é público (sem client_secret, ver
      // infra/keycloak/realm-financas.json) — Auth.js usa PKCE + state por
      // padrão pra qualquer provider OIDC, então authorization_code sem
      // client_secret continua seguro. Sem isso o Auth.js tentaria enviar
      // Basic Auth com um client_secret undefined e o Keycloak rejeitaria.
      client: { token_endpoint_auth_method: "none" },
      // "wellKnown" propositalmente omitido: com token/userinfo abaixo já
      // resolvidos, o Auth.js pula a chamada de discovery inteiramente
      // (ver node_modules/@auth/core/src/lib/actions/callback/oauth/callback.ts
      // e .../signin/authorization-url.ts) — sem isso o discovery document
      // do Keycloak sempre devolveria URLs "localhost" (mesmo KC_HOSTNAME
      // fixo que estabiliza o "iss"), inalcançáveis de dentro do container.
      // "authorization" é a única chamada que o navegador precisa alcançar
      // direto (redirect de login), por isso usa o issuer público; token/
      // userinfo/jwks são chamados só pelo servidor, por isso usam o interno.
      authorization: `${issuer}/protocol/openid-connect/auth`,
      token: `${issuerInterno}/protocol/openid-connect/token`,
      userinfo: `${issuerInterno}/protocol/openid-connect/userinfo`,
      jwks_endpoint: `${issuerInterno}/protocol/openid-connect/certs`,
    }),
  ],
  session: { strategy: "jwt" },
  pages: {
    signIn: "/login",
  },
  callbacks: {
    // Usado pelo proxy.ts (export { auth as proxy }) — retornar false
    // redireciona automaticamente pra pages.signIn acima. "/login" precisa
    // ficar liberado, senão vira loop de redirecionamento.
    authorized({ auth, request }) {
      const estaNoLogin = request.nextUrl.pathname.startsWith("/login");
      if (estaNoLogin) return true;
      return !!auth?.user;
    },
    async jwt({ token, account, profile }) {
      if (account && profile) {
        // sub do id_token = usuarioId em todo o resto do sistema (ADR-0003).
        // NUNCA usar o user.id que o Auth.js gera sozinho aqui — sem adapter
        // de banco configurado, ele é um UUID aleatório novo a cada login,
        // não o "sub" real do Keycloak (achado lendo o código-fonte do
        // @auth/core, ver docs/historico.md).
        token.sub = profile.sub as string;
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.idToken = account.id_token as string | undefined;
        token.expiresAt = account.expires_at;
        delete token.error;
        return token;
      }

      if (!precisaRenovar(token.expiresAt)) {
        return token;
      }

      if (!token.refreshToken) {
        token.error = "SemRefreshToken";
        return token;
      }

      try {
        const renovado = await renovarToken(
          token.refreshToken,
          issuerInterno,
          clientId
        );
        token.accessToken = renovado.accessToken;
        token.refreshToken = renovado.refreshToken;
        token.idToken = renovado.idToken;
        token.expiresAt = renovado.expiresAt;
        delete token.error;
      } catch {
        token.error = "FalhaAoRenovarToken";
      }

      return token;
    },
    // accessToken/refreshToken/idToken NUNCA entram aqui — este objeto é o
    // que /api/auth/session devolve pro navegador (useSession() em Client
    // Component). Route Handler/Server Action que precisa do access token
    // usa lib/auth-token.ts (getToken(), lê o cookie httpOnly direto,
    // nunca passa pelo endpoint público de sessão). Ver ADR-0027.
    async session({ session, token }) {
      if (token.sub) {
        session.user.id = token.sub;
      }
      if (token.error) {
        session.error = token.error;
      }
      return session;
    },
  },
});

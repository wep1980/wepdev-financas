import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { entrar } from "@/lib/auth-actions";
import { Button } from "@/components/ui/button";
import { AppSidebar } from "./app-sidebar";

// Casco compartilhado por toda rota autenticada (proxy.ts já garante
// sessão — mas cai pro /login mesmo assim se por algum motivo não
// tiver usuário, ex: sessão expirou e a renovação falhou).
export default async function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const sessao = await auth();
  if (!sessao?.user) {
    redirect("/login");
  }

  // `sessao.error` ("SemRefreshToken"/"FalhaAoRenovarToken", ver auth.ts)
  // significa que o access token não é mais válido e não dá pra renovar —
  // qualquer página abaixo que chame um microsserviço ia quebrar com 401
  // não tratado (achado real, 2026-08-11: a página crashava direto, sem
  // chegar a mostrar aviso nenhum, porque `children` continuava renderizando
  // e disparando as chamadas). Corrigido cortando o render de `children`
  // aqui — mesmo padrão de guarda que `/login` já usa (`if (!sessao?.user)`
  // acima), só que pro caso "tem usuário, mas o token morreu".
  if (sessao.error) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-16 text-center">
        <p className="text-muted-foreground max-w-md">
          Sua sessão expirou. Entre novamente pra continuar.
        </p>
        <form action={entrar}>
          <Button type="submit" size="lg">
            Entrar novamente
          </Button>
        </form>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen">
      <AppSidebar nome={sessao.user.name ?? sessao.user.email ?? ""} />
      <div className="flex flex-1 flex-col">
        {/* pt-16 no mobile evita o conteúdo colidir com o botão de menu
            flutuante (fixed top-4 left-4) do AppSidebar. */}
        <main className="flex flex-1 flex-col pt-16 md:pt-0">{children}</main>
      </div>
    </div>
  );
}

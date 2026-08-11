import { redirect } from "next/navigation";
import { auth } from "@/auth";
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

  return (
    <div className="flex min-h-screen">
      <AppSidebar nome={sessao.user.name ?? sessao.user.email ?? ""} />
      <div className="flex flex-1 flex-col">
        {sessao.error && (
          <div className="bg-destructive/10 text-destructive px-6 py-2 text-center text-sm">
            Sessão com problema, entre novamente
          </div>
        )}
        {/* pt-16 no mobile evita o conteúdo colidir com o botão de menu
            flutuante (fixed top-4 left-4) do AppSidebar. */}
        <main className="flex flex-1 flex-col pt-16 md:pt-0">{children}</main>
      </div>
    </div>
  );
}

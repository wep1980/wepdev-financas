import { redirect } from "next/navigation";
import { auth } from "@/auth";
import { entrar } from "@/lib/auth-actions";
import { Button } from "@/components/ui/button";

export default async function LoginPage() {
  const sessao = await auth();
  if (sessao?.user) {
    redirect("/");
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-6 p-16 text-center">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">
          Sistema de Finanças Pessoais
        </h1>
        <p className="text-muted-foreground max-w-md">
          Entre com sua conta pra gerenciar contas, transações e conversar
          com o assistente de IA.
        </p>
      </div>
      <form action={entrar}>
        <Button type="submit" size="lg">
          Entrar
        </Button>
      </form>
    </div>
  );
}

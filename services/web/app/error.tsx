"use client";

import { Button } from "@/components/ui/button";
import { entrar } from "@/lib/auth-actions";

// Rede de segurança pra qualquer erro não tratado numa página (ex: 401 de
// sessão expirada — o caso mais comum, ver app/(app)/layout.tsx, que evita
// a maioria antes de chegar aqui — mas também microsserviço fora do ar ou
// bug real). Sem isso, o Next.js aborta a conexão no meio do streaming em
// vez de mostrar uma página de erro, e o navegador mostra uma mensagem
// genérica ("This page couldn't load") sem nenhuma ação possível (achado
// real, 2026-08-11). Erro/stack detalhado nunca chega aqui em produção —
// Next.js só entrega `digest`, de propósito, pra não vazar dado sensível.
export default function ErrorPage({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-16 text-center">
      <h1 className="text-xl font-semibold tracking-tight">
        Algo deu errado
      </h1>
      <p className="text-muted-foreground max-w-md">
        Não foi possível carregar esta página. Pode ser um problema
        temporário — tenta de novo, ou entra na conta de novo se persistir.
      </p>
      <div className="flex gap-3">
        <Button variant="outline" onClick={reset}>
          Tentar de novo
        </Button>
        <form action={entrar}>
          <Button type="submit">Entrar novamente</Button>
        </form>
      </div>
      {error.digest && (
        <p className="text-muted-foreground text-xs">
          Código: {error.digest}
        </p>
      )}
    </div>
  );
}

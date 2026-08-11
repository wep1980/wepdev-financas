"use client";

import "./globals.css";
import { Button } from "@/components/ui/button";

// Só dispara se o RootLayout em si (app/layout.tsx) quebrar — error.tsx
// normal não cobre esse caso, precisa da própria tag <html>/<body> porque
// substitui o layout inteiro. Sem Server Action aqui de propósito (login
// via `entrar()` depende de infra que pode ser justamente o que falhou);
// só um reload simples.
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html lang="pt-BR" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <div className="flex flex-1 flex-col items-center justify-center gap-4 p-16 text-center">
          <h1 className="text-xl font-semibold tracking-tight">
            Algo deu errado
          </h1>
          <p className="text-muted-foreground max-w-md">
            Não foi possível carregar o sistema. Tenta recarregar a página.
          </p>
          <Button onClick={reset}>Tentar de novo</Button>
          {error.digest && (
            <p className="text-muted-foreground text-xs">
              Código: {error.digest}
            </p>
          )}
        </div>
      </body>
    </html>
  );
}

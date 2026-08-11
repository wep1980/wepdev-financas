"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Menu, X } from "lucide-react";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { NAV_ITEMS } from "@/lib/nav-items";
import { sair } from "@/lib/auth-actions";

/**
 * Menu lateral fixo (desktop) / off-canvas (mobile) — decisão de
 * refinamento visual, 2026-08-10 (docs/architecture/design-system.md):
 * referências (Linear/Vercel/Notion/YNAB) usam sidebar, não header
 * horizontal (o que tinha desde o item 3). CSS puro (breakpoint `md:`)
 * pro estado desktop, sem hook de JS pra detectar mobile — evita
 * flash de layout errado no primeiro paint. Escrito na mão em vez de
 * importar o bloco "sidebar" do shadcn/ui — aquele vem com
 * collapse-to-icon, atalho de teclado, cookie de estado, tooltip por
 * item: infraestrutura de dashboard enterprise que 5 itens de menu não
 * precisam (mesmo raciocínio de não superdimensionar aplicado ao
 * pedido de design system completo).
 */
export function AppSidebar({ nome }: { nome: string }) {
  const [aberto, setAberto] = useState(false);
  const pathname = usePathname();

  return (
    <>
      <button
        type="button"
        onClick={() => setAberto(true)}
        aria-label="Abrir menu"
        className="bg-card ring-foreground/10 fixed top-4 left-4 z-40 flex size-9 items-center justify-center rounded-lg shadow-sm ring-1 md:hidden"
      >
        <Menu className="size-5" />
      </button>

      {aberto && (
        <div
          className="fixed inset-0 z-40 bg-black/30 md:hidden"
          onClick={() => setAberto(false)}
          aria-hidden="true"
        />
      )}

      <aside
        className={cn(
          "bg-sidebar text-sidebar-foreground border-sidebar-border fixed inset-y-0 left-0 z-50 flex w-64 flex-col border-r transition-transform duration-200 ease-in-out md:static md:translate-x-0",
          aberto ? "translate-x-0" : "-translate-x-full"
        )}
      >
        <div className="flex items-center justify-between px-6 py-5">
          <span className="text-lg font-semibold tracking-tight">
            Finanças
          </span>
          <button
            type="button"
            onClick={() => setAberto(false)}
            aria-label="Fechar menu"
            className="md:hidden"
          >
            <X className="size-5" />
          </button>
        </div>

        <nav className="flex flex-1 flex-col gap-1 px-3">
          {NAV_ITEMS.map((item) => {
            if (!item.implementado) {
              return (
                <span
                  key={item.href}
                  className="text-sidebar-foreground/40 flex items-center justify-between rounded-lg px-3 py-2 text-sm"
                  title="Ainda não implementado"
                >
                  {item.label}
                  <span className="text-[0.65rem] uppercase">em breve</span>
                </span>
              );
            }

            const ativo = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setAberto(false)}
                className={cn(
                  "rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  ativo
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/80 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground"
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="border-sidebar-border flex flex-col gap-2 border-t px-3 py-4">
          <span className="text-sidebar-foreground/70 truncate px-3 text-sm">
            {nome}
          </span>
          <form action={sair}>
            <Button type="submit" variant="outline" size="sm" className="w-full">
              Sair
            </Button>
          </form>
        </div>
      </aside>
    </>
  );
}

import Link from "next/link";
import { buscarConfiguracaoIa, listarConversas } from "@/lib/ai-service";
import { Button } from "@/components/ui/button";
import { ConfiguracaoIaDialog } from "./configuracao-ia-dialog";

const FORMATADOR_DATA = new Intl.DateTimeFormat("pt-BR", {
  day: "2-digit",
  month: "2-digit",
});

export default async function ChatLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [conversas, configuracao] = await Promise.all([
    listarConversas(),
    buscarConfiguracaoIa(),
  ]);

  return (
    <div className="flex flex-1">
      <aside className="border-border hidden w-64 flex-col border-r md:flex">
        <div className="flex items-center justify-between px-4 py-4">
          <span className="font-semibold">Conversas</span>
          <Link href="/chat">
            <Button variant="outline" size="sm">
              Nova
            </Button>
          </Link>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto px-2">
          {conversas.length === 0 ? (
            <p className="text-muted-foreground px-2 text-sm">
              Nenhuma conversa ainda.
            </p>
          ) : (
            conversas.map((conversa) => (
              <Link
                key={conversa.id}
                href={`/chat/${conversa.id}`}
                className="hover:bg-muted flex flex-col gap-0.5 rounded-lg px-2 py-2 text-sm"
              >
                <span className="truncate">
                  {conversa.ultimaMensagemPreview || "(sem mensagens)"}
                </span>
                <span className="text-muted-foreground text-xs">
                  {FORMATADOR_DATA.format(new Date(conversa.ultimaMensagemEm))}
                </span>
              </Link>
            ))
          )}
        </nav>
        <div className="border-border border-t p-3">
          <ConfiguracaoIaDialog
            configuracao={configuracao}
            trigger={
              <Button variant="outline" size="sm" className="w-full">
                {configuracao.configurado ? "IA configurada" : "Configurar IA"}
              </Button>
            }
          />
        </div>
      </aside>
      <div className="flex flex-1 flex-col">
        {!configuracao.configurado && (
          <div className="bg-muted text-muted-foreground px-6 py-2 text-center text-sm">
            Configure um provedor de IA (canto inferior esquerdo) antes de
            conversar.
          </div>
        )}
        {children}
      </div>
    </div>
  );
}

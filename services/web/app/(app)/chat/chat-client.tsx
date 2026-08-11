"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { cn } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import type { AcaoProposta, MensagemConversa } from "@/lib/ai-service";
import { enviarMensagemAction } from "./actions";

interface MensagemExibida {
  autor: "USUARIO" | "AGENTE";
  texto: string;
  tipo?: string | null;
  acaoProposta?: AcaoProposta | null;
}

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const FORMATADOR_HORA = new Intl.DateTimeFormat("pt-BR", {
  hour: "2-digit",
  minute: "2-digit",
});

export function ChatClient({
  conversaId: conversaIdInicial,
  mensagensIniciais,
  iaConfigurada,
}: {
  conversaId?: string;
  mensagensIniciais: MensagemConversa[];
  iaConfigurada: boolean;
}) {
  const router = useRouter();
  const [conversaId, setConversaId] = useState(conversaIdInicial);
  const [mensagens, setMensagens] = useState<MensagemExibida[]>(
    mensagensIniciais.map((m) => ({
      autor: m.autor,
      texto: m.texto,
      tipo: m.tipo,
    }))
  );
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const fimRef = useRef<HTMLDivElement>(null);

  async function enviar(mensagemTexto: string) {
    const conteudo = mensagemTexto.trim();
    if (!conteudo || enviando) return;

    setErro(null);
    setMensagens((atual) => [...atual, { autor: "USUARIO", texto: conteudo }]);
    setTexto("");
    setEnviando(true);

    try {
      const resultado = await enviarMensagemAction(conteudo, conversaId);
      setMensagens((atual) => [
        ...atual,
        {
          autor: "AGENTE",
          texto: resultado.resposta,
          tipo: resultado.tipo,
          acaoProposta: resultado.acaoProposta,
        },
      ]);
      if (!conversaId) {
        setConversaId(resultado.conversaId);
        router.replace(`/chat/${resultado.conversaId}`);
      }
    } catch (e) {
      setErro(e instanceof Error ? e.message : "Falha ao enviar mensagem");
    } finally {
      setEnviando(false);
      requestAnimationFrame(() =>
        fimRef.current?.scrollIntoView({ behavior: "smooth" })
      );
    }
  }

  return (
    <div className="flex flex-1 flex-col">
      <div className="flex-1 space-y-4 overflow-y-auto p-6 md:p-8">
        {mensagens.length === 0 && (
          <p className="text-muted-foreground text-sm">
            Pergunte sobre sua situação financeira (ex: &quot;quanto tenho
            disponível pra gastar esse mês?&quot;) ou peça pra registrar algo
            (ex: &quot;lança uma despesa de 50 reais em Mercado hoje&quot;).
          </p>
        )}
        {mensagens.map((mensagem, indice) => (
          <div
            key={indice}
            className={cn(
              "flex",
              mensagem.autor === "USUARIO" ? "justify-end" : "justify-start"
            )}
          >
            <div
              className={cn(
                "max-w-lg rounded-xl px-4 py-2.5 text-sm",
                mensagem.autor === "USUARIO"
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-foreground"
              )}
            >
              <p className="whitespace-pre-wrap">{mensagem.texto}</p>
              {mensagem.acaoProposta && (
                <PropostaAcaoCard
                  acaoProposta={mensagem.acaoProposta}
                  onConfirmar={() => enviar("sim")}
                  desabilitado={enviando}
                />
              )}
            </div>
          </div>
        ))}
        {enviando && (
          <p className="text-muted-foreground text-sm">Pensando...</p>
        )}
        {erro && <p className="text-destructive text-sm">{erro}</p>}
        <div ref={fimRef} />
      </div>

      <form
        onSubmit={(evento) => {
          evento.preventDefault();
          enviar(texto);
        }}
        className="border-border flex items-center gap-2 border-t p-4"
      >
        <Input
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          placeholder={
            iaConfigurada
              ? "Digite sua mensagem..."
              : "Configure um provedor de IA acima antes de conversar"
          }
          disabled={enviando}
        />
        <Button type="submit" disabled={enviando || !texto.trim()}>
          Enviar
        </Button>
      </form>
    </div>
  );
}

function PropostaAcaoCard({
  acaoProposta,
  onConfirmar,
  desabilitado,
}: {
  acaoProposta: AcaoProposta;
  onConfirmar: () => void;
  desabilitado: boolean;
}) {
  return (
    <div className="border-border bg-card text-card-foreground mt-2 flex flex-col gap-1 rounded-lg border p-3 text-sm">
      <div className="flex items-center justify-between">
        <span className="font-medium">{acaoProposta.descricao}</span>
        <span
          className={cn(
            "font-medium tabular-nums",
            acaoProposta.tipo === "DESPESA" ? "text-destructive" : "text-primary"
          )}
        >
          {acaoProposta.tipo === "DESPESA" ? "-" : "+"}
          {FORMATADOR_MOEDA.format(acaoProposta.valor)}
        </span>
      </div>
      {acaoProposta.categoria && (
        <span className="text-muted-foreground text-xs">
          {acaoProposta.categoria}
          {acaoProposta.recorrente ? " · recorrente (mensal)" : ""}
        </span>
      )}
      <span className="text-muted-foreground text-xs">
        Expira às {FORMATADOR_HORA.format(new Date(acaoProposta.expiraEm))} — responda
        &quot;sim&quot; ou corrija a mensagem
      </span>
      <Button
        type="button"
        size="sm"
        variant="outline"
        className="mt-1 self-start"
        onClick={onConfirmar}
        disabled={desabilitado}
      >
        Confirmar
      </Button>
    </div>
  );
}

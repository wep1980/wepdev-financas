"use client";

import { useActionState, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import type { DocumentoImportado, LancamentoPendente } from "@/lib/document-service";
import { buscarDocumentoAction, confirmarLancamentosAction, type ConfirmarFormState } from "../actions";

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const ESTADO_INICIAL: ConfirmarFormState = {};

const INTERVALO_POLLING_MS = 4000;
const STATUS_EM_ANDAMENTO = new Set(["RECEBIDO", "PROCESSANDO"]);

const TEXTO_POR_STATUS: Record<string, string> = {
  RECEBIDO: "Fatura recebida, preparando a extração...",
  PROCESSANDO: "Lendo a fatura e identificando os lançamentos...",
};

function rotuloParcela(lancamento: LancamentoPendente) {
  return lancamento.quantidadeParcelas > 1
    ? `Parcela ${lancamento.numeroParcela}/${lancamento.quantidadeParcelas}`
    : "À vista";
}

/** Soma só as despesas selecionadas — mesmo filtro do que de fato é
 * lançado no cartão ao confirmar (RECEITA/estorno não é lançado, ver
 * ConfirmarLancamentosUseCase no document-service, ADR-0028). */
function calcularTotalALancar(lancamentos: LancamentoPendente[], idsSelecionados: Set<string>) {
  return lancamentos
    .filter((l) => idsSelecionados.has(l.id) && l.tipo === "DESPESA")
    .reduce((total, l) => total + l.valor, 0);
}

export function DocumentoDetalhe({
  documentoInicial,
}: {
  documentoInicial: DocumentoImportado;
}) {
  const [documento, setDocumento] = useState(documentoInicial);

  useEffect(() => {
    if (!STATUS_EM_ANDAMENTO.has(documento.status)) return;

    const intervalo = setInterval(async () => {
      const atualizado = await buscarDocumentoAction(documento.id);
      setDocumento(atualizado);
    }, INTERVALO_POLLING_MS);

    return () => clearInterval(intervalo);
  }, [documento.status, documento.id]);

  if (STATUS_EM_ANDAMENTO.has(documento.status)) {
    return (
      <div className="flex flex-col gap-2">
        <Progress />
        <p className="text-muted-foreground text-sm">
          {TEXTO_POR_STATUS[documento.status]} Isso pode levar alguns
          minutos — a página atualiza sozinha, não precisa recarregar.
        </p>
      </div>
    );
  }

  if (documento.status === "ERRO_PROCESSAMENTO") {
    return (
      <p className="text-destructive">
        Falha ao processar: {documento.mensagemErro ?? "erro desconhecido"}
      </p>
    );
  }

  if (documento.status === "CONFIRMADO") {
    return (
      <div className="flex flex-col gap-3">
        <p className="text-muted-foreground text-sm">
          Compras novas foram lançadas no cartão (à vista ou parceladas) —
          compra já conhecida de um upload anterior foi ignorada. O valor
          só sai da conta quando a fatura for paga (aba Cartões).
        </p>
        <ul className="divide-border border-border divide-y rounded-lg border">
          {documento.lancamentos.map((lancamento) => (
            <li key={lancamento.id} className="flex items-center justify-between px-4 py-3">
              <div className="flex flex-col">
                <span>{lancamento.descricao}</span>
                <span className="text-muted-foreground text-sm">
                  {rotuloParcela(lancamento)}
                  {lancamento.tipo === "RECEITA" ? " · estorno, não lançado" : ""}
                </span>
              </div>
              <div className="flex items-center gap-4">
                <span className="font-medium tabular-nums">
                  {FORMATADOR_MOEDA.format(lancamento.valor)}
                </span>
                <span className="text-muted-foreground text-sm">
                  {lancamento.status === "CONFIRMADO" ? "Confirmado" : "Rejeitado"}
                </span>
              </div>
            </li>
          ))}
        </ul>
      </div>
    );
  }

  // AGUARDANDO_CONFIRMACAO
  return <ConfirmarLancamentosForm documento={documento} />;
}

function ConfirmarLancamentosForm({ documento }: { documento: DocumentoImportado }) {
  const [state, formAction, pending] = useActionState(
    confirmarLancamentosAction,
    ESTADO_INICIAL
  );
  const formRef = useRef<HTMLFormElement>(null);
  const pendentes = documento.lancamentos.filter((l) => l.status === "PENDENTE");
  const [selecionados, setSelecionados] = useState(
    () => new Set(pendentes.map((l) => l.id))
  );

  function alternar(id: string) {
    setSelecionados((atual) => {
      const proximo = new Set(atual);
      if (proximo.has(id)) {
        proximo.delete(id);
      } else {
        proximo.add(id);
      }
      return proximo;
    });
  }

  const totalALancar = useMemo(
    () => calcularTotalALancar(pendentes, selecionados),
    [pendentes, selecionados]
  );

  return (
    <form ref={formRef} action={formAction} className="flex flex-col gap-4">
      <input type="hidden" name="documentoId" value={documento.id} />

      <div className="divide-border border-border divide-y rounded-lg border">
        {pendentes.map((lancamento) => (
          <label
            key={lancamento.id}
            className="hover:bg-muted/50 flex cursor-pointer items-center gap-3 px-4 py-3"
          >
            <input
              type="checkbox"
              name="lancamentoIdsConfirmados"
              value={lancamento.id}
              checked={selecionados.has(lancamento.id)}
              onChange={() => alternar(lancamento.id)}
              className="size-4"
            />
            <div className="flex flex-1 flex-col">
              <span>{lancamento.descricao}</span>
              <span className="text-muted-foreground text-sm">
                {lancamento.data} · {lancamento.categoriaSugerida ?? "Sem categoria"} ·{" "}
                {rotuloParcela(lancamento)}
              </span>
            </div>
            <span
              className={`font-medium tabular-nums ${
                lancamento.tipo === "DESPESA" ? "text-destructive" : "text-primary"
              }`}
            >
              {lancamento.tipo === "DESPESA" ? "-" : "+"}
              {FORMATADOR_MOEDA.format(lancamento.valor)}
            </span>
          </label>
        ))}
      </div>

      <div className="bg-muted flex items-center justify-between rounded-lg px-4 py-3">
        <span className="font-medium">Total a lançar no cartão:</span>
        <span className="text-destructive font-medium tabular-nums">
          {FORMATADOR_MOEDA.format(totalALancar)}
        </span>
      </div>

      {state.erro && <p className="text-destructive text-sm">{state.erro}</p>}
      <Button
        type="submit"
        disabled={pending || selecionados.size === 0}
        className="self-start"
      >
        {pending ? "Confirmando..." : "Confirmar"}
      </Button>
    </form>
  );
}

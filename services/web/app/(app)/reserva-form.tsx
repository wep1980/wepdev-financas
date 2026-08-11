"use client";

import { useActionState, useRef } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { definirReservaAction, type FormState } from "./actions";

const ESTADO_INICIAL: FormState = {};

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

export function ReservaForm({
  valorAtual,
  sugestao,
}: {
  valorAtual: number;
  sugestao: number;
}) {
  const [state, formAction, pending] = useActionState(
    definirReservaAction,
    ESTADO_INICIAL
  );
  const inputRef = useRef<HTMLInputElement>(null);

  return (
    <form action={formAction} className="flex flex-col gap-2">
      <Label htmlFor="valor" className="text-muted-foreground text-xs">
        Reserva (descontada do disponível pra gastar)
      </Label>
      <div className="flex gap-2">
        <Input
          ref={inputRef}
          id="valor"
          name="valor"
          type="number"
          step="0.01"
          min="0"
          defaultValue={valorAtual}
          className="w-32"
        />
        <Button type="submit" variant="outline" size="sm" disabled={pending}>
          {pending ? "Salvando..." : "Salvar"}
        </Button>
      </div>
      {state.erro && <p className="text-destructive text-sm">{state.erro}</p>}
      {sugestao > 0 && (
        <p className="text-muted-foreground text-xs">
          Sugestão: {FORMATADOR_MOEDA.format(sugestao)} (média de receita
          confirmada dos últimos 3 meses) —{" "}
          <button
            type="button"
            className="text-primary underline underline-offset-2"
            onClick={() => {
              if (inputRef.current) {
                inputRef.current.value = sugestao.toFixed(2);
              }
            }}
          >
            usar sugestão
          </button>
        </p>
      )}
    </form>
  );
}

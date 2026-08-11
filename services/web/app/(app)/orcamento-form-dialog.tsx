"use client";

import { useActionState, useEffect, useRef, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import type { Orcamento } from "@/lib/budget-service";
import {
  atualizarOrcamentoAction,
  criarOrcamentoAction,
  type FormState,
} from "./actions";

const ESTADO_INICIAL: FormState = {};

type Props =
  | { trigger: React.ReactNode; modo: "criar"; mes: string }
  | { trigger: React.ReactNode; modo: "editar"; orcamento: Orcamento };

export function OrcamentoFormDialog(props: Props) {
  const [open, setOpen] = useState(false);
  const acao = props.modo === "criar" ? criarOrcamentoAction : atualizarOrcamentoAction;
  const [state, formAction, pending] = useActionState(acao, ESTADO_INICIAL);
  const formRef = useRef<HTMLFormElement>(null);
  const enviandoRef = useRef(false);

  useEffect(() => {
    if (enviandoRef.current && !pending && !state.erro) {
      setOpen(false);
      formRef.current?.reset();
    }
    enviandoRef.current = pending;
  }, [pending, state.erro]);

  const orcamento = props.modo === "editar" ? props.orcamento : undefined;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<span>{props.trigger}</span>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {props.modo === "criar" ? "Novo orçamento" : "Editar limite"}
          </DialogTitle>
        </DialogHeader>
        <form ref={formRef} action={formAction} className="flex flex-col gap-3">
          {orcamento && <input type="hidden" name="id" value={orcamento.id} />}
          {props.modo === "criar" && (
            <>
              <input type="hidden" name="mesReferencia" value={props.mes} />
              <div className="flex flex-col gap-1.5">
                <Label htmlFor="categoria">Categoria</Label>
                <Input id="categoria" name="categoria" required />
              </div>
            </>
          )}
          {orcamento && (
            <p className="text-muted-foreground text-sm">
              {orcamento.categoria} · {orcamento.mesReferencia}
            </p>
          )}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="valorLimite">Limite mensal</Label>
            <Input
              id="valorLimite"
              name="valorLimite"
              type="number"
              step="0.01"
              min="0.01"
              defaultValue={orcamento?.valorLimite}
              required
            />
          </div>
          {state.erro && <p className="text-destructive text-sm">{state.erro}</p>}
          <DialogFooter>
            <Button type="submit" disabled={pending}>
              {pending ? "Salvando..." : "Salvar"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

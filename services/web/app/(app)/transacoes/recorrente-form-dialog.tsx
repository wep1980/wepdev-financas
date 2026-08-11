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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import type { Conta } from "@/lib/account-service";
import { criarTransacaoRecorrenteAction, type FormState } from "./actions";

const ESTADO_INICIAL: FormState = {};

export function RecorrenteFormDialog({
  trigger,
  contas,
}: {
  trigger: React.ReactNode;
  contas: Conta[];
}) {
  const [open, setOpen] = useState(false);
  const [state, formAction, pending] = useActionState(
    criarTransacaoRecorrenteAction,
    ESTADO_INICIAL
  );
  const formRef = useRef<HTMLFormElement>(null);
  const enviandoRef = useRef(false);

  useEffect(() => {
    if (enviandoRef.current && !pending && !state.erro) {
      setOpen(false);
      formRef.current?.reset();
    }
    enviandoRef.current = pending;
  }, [pending, state.erro]);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<span>{trigger}</span>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Nova regra recorrente</DialogTitle>
        </DialogHeader>
        <form ref={formRef} action={formAction} className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="r-contaId">Conta</Label>
            <Select name="contaId" defaultValue={contas[0]?.id}>
              <SelectTrigger id="r-contaId" className="w-full">
                <SelectValue placeholder="Selecione a conta" />
              </SelectTrigger>
              <SelectContent>
                {contas.map((conta) => (
                  <SelectItem key={conta.id} value={conta.id}>
                    {conta.nome}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="r-descricao">Descrição</Label>
            <Input id="r-descricao" name="descricao" required />
          </div>

          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="r-valor">Valor</Label>
              <Input
                id="r-valor"
                name="valor"
                type="number"
                step="0.01"
                min="0.01"
                required
              />
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="r-tipo">Tipo</Label>
              <Select name="tipo" defaultValue="DESPESA">
                <SelectTrigger id="r-tipo" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DESPESA">Despesa</SelectItem>
                  <SelectItem value="RECEITA">Receita</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="r-categoria">Categoria</Label>
            <Input id="r-categoria" name="categoria" />
          </div>

          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="r-dataInicio">Início</Label>
              <Input id="r-dataInicio" name="dataInicio" type="date" required />
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="r-quantidadeOcorrencias">
                Nº de vezes (vazio = indefinido)
              </Label>
              <Input
                id="r-quantidadeOcorrencias"
                name="quantidadeOcorrencias"
                type="number"
                min="1"
              />
            </div>
          </div>

          <p className="text-muted-foreground text-xs">
            Frequência mensal (única opção disponível no momento).
          </p>

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

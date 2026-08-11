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
import type { Transacao } from "@/lib/transaction-service";
import {
  atualizarTransacaoAction,
  criarTransacaoAction,
  type FormState,
} from "./actions";

const ESTADO_INICIAL: FormState = {};

type Props =
  | { trigger: React.ReactNode; modo: "criar"; contas: Conta[] }
  | { trigger: React.ReactNode; modo: "editar"; transacao: Transacao };

export function TransacaoFormDialog(props: Props) {
  const [open, setOpen] = useState(false);
  const acao = props.modo === "criar" ? criarTransacaoAction : atualizarTransacaoAction;
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

  const transacao = props.modo === "editar" ? props.transacao : undefined;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<span>{props.trigger}</span>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {props.modo === "criar" ? "Nova transação" : "Editar transação"}
          </DialogTitle>
        </DialogHeader>
        <form ref={formRef} action={formAction} className="flex flex-col gap-3">
          {transacao && <input type="hidden" name="id" value={transacao.id} />}

          {props.modo === "criar" && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="contaId">Conta</Label>
              <Select name="contaId" defaultValue={props.contas[0]?.id}>
                <SelectTrigger id="contaId" className="w-full">
                  <SelectValue placeholder="Selecione a conta" />
                </SelectTrigger>
                <SelectContent>
                  {props.contas.map((conta) => (
                    <SelectItem key={conta.id} value={conta.id}>
                      {conta.nome}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}

          <div className="flex flex-col gap-1.5">
            <Label htmlFor="descricao">Descrição</Label>
            <Input
              id="descricao"
              name="descricao"
              defaultValue={transacao?.descricao}
              required
            />
          </div>

          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="valor">Valor</Label>
              <Input
                id="valor"
                name="valor"
                type="number"
                step="0.01"
                min="0.01"
                defaultValue={transacao?.valor}
                required
              />
            </div>
            {props.modo === "criar" && (
              <div className="flex flex-1 flex-col gap-1.5">
                <Label htmlFor="tipo">Tipo</Label>
                <Select name="tipo" defaultValue="DESPESA">
                  <SelectTrigger id="tipo" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DESPESA">Despesa</SelectItem>
                    <SelectItem value="RECEITA">Receita</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>

          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="categoria">Categoria</Label>
              <Input
                id="categoria"
                name="categoria"
                defaultValue={transacao?.categoria}
              />
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="dataTransacao">Data</Label>
              <Input
                id="dataTransacao"
                name="dataTransacao"
                type="date"
                defaultValue={transacao?.dataTransacao}
              />
            </div>
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

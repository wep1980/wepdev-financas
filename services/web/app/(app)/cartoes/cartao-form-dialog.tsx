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
import type { Bandeira } from "@/lib/card-service";
import { criarCartaoAction, type FormState } from "./actions";

const BANDEIRAS: { value: Bandeira; label: string }[] = [
  { value: "VISA", label: "Visa" },
  { value: "MASTERCARD", label: "Mastercard" },
  { value: "ELO", label: "Elo" },
  { value: "AMEX", label: "Amex" },
  { value: "OUTRA", label: "Outra" },
];

const ESTADO_INICIAL: FormState = {};

export function CartaoFormDialog({
  trigger,
  contas,
}: {
  trigger: React.ReactNode;
  contas: Conta[];
}) {
  const [open, setOpen] = useState(false);
  const [state, formAction, pending] = useActionState(criarCartaoAction, ESTADO_INICIAL);
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
          <DialogTitle>Novo cartão</DialogTitle>
        </DialogHeader>
        <form ref={formRef} action={formAction} className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="apelido">Apelido</Label>
            <Input id="apelido" name="apelido" placeholder="Nubank" required />
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="bandeira">Bandeira</Label>
            <Select name="bandeira" defaultValue="VISA">
              <SelectTrigger id="bandeira" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {BANDEIRAS.map((b) => (
                  <SelectItem key={b.value} value={b.value}>
                    {b.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="limite">Limite</Label>
            <Input id="limite" name="limite" type="number" step="0.01" placeholder="0,00" required />
          </div>
          <div className="flex gap-3">
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="diaFechamento">Dia de fechamento</Label>
              <Input id="diaFechamento" name="diaFechamento" type="number" min={1} max={31} required />
            </div>
            <div className="flex flex-1 flex-col gap-1.5">
              <Label htmlFor="diaVencimento">Dia de vencimento</Label>
              <Input id="diaVencimento" name="diaVencimento" type="number" min={1} max={31} required />
            </div>
          </div>
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="contaPagamentoId">Conta que paga a fatura</Label>
            <Select name="contaPagamentoId" defaultValue={contas[0]?.id}>
              <SelectTrigger id="contaPagamentoId" className="w-full">
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
          {state.erro && <p className="text-destructive text-sm">{state.erro}</p>}
          <DialogFooter>
            <Button type="submit" disabled={pending || contas.length === 0}>
              {pending ? "Salvando..." : "Salvar"}
            </Button>
          </DialogFooter>
          {contas.length === 0 && (
            <p className="text-muted-foreground text-sm">
              Crie uma conta primeiro (aba Contas) antes de cadastrar um cartão.
            </p>
          )}
        </form>
      </DialogContent>
    </Dialog>
  );
}

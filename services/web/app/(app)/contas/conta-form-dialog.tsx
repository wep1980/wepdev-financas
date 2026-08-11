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
import {
  atualizarContaAction,
  criarContaAction,
  type FormState,
} from "./actions";

const TIPOS: { value: Conta["tipo"]; label: string }[] = [
  { value: "CORRENTE", label: "Conta corrente" },
  { value: "POUPANCA", label: "Poupança" },
  { value: "CARTEIRA", label: "Carteira" },
  { value: "CARTAO_CREDITO", label: "Cartão de crédito" },
  { value: "INVESTIMENTO", label: "Investimento" },
];

const ESTADO_INICIAL: FormState = {};

type Props =
  | { trigger: React.ReactNode; modo: "criar" }
  | { trigger: React.ReactNode; modo: "editar"; conta: Conta };

export function ContaFormDialog(props: Props) {
  const [open, setOpen] = useState(false);
  const acao = props.modo === "criar" ? criarContaAction : atualizarContaAction;
  const [state, formAction, pending] = useActionState(acao, ESTADO_INICIAL);
  const formRef = useRef<HTMLFormElement>(null);
  const enviandoRef = useRef(false);

  // useActionState não expõe "sucesso" diretamente — detecta a transição
  // pending:true -> false sem erro pra fechar o dialog e limpar o form.
  useEffect(() => {
    if (enviandoRef.current && !pending && !state.erro) {
      setOpen(false);
      formRef.current?.reset();
    }
    enviandoRef.current = pending;
  }, [pending, state.erro]);

  const conta = props.modo === "editar" ? props.conta : undefined;

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<span>{props.trigger}</span>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {props.modo === "criar" ? "Nova conta" : "Editar conta"}
          </DialogTitle>
        </DialogHeader>
        <form ref={formRef} action={formAction} className="flex flex-col gap-3">
          {conta && <input type="hidden" name="id" value={conta.id} />}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="nome">Nome</Label>
            <Input
              id="nome"
              name="nome"
              defaultValue={conta?.nome}
              required
            />
          </div>
          {props.modo === "criar" && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="tipo">Tipo</Label>
              <Select name="tipo" defaultValue="CORRENTE">
                <SelectTrigger id="tipo" className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {TIPOS.map((t) => (
                    <SelectItem key={t.value} value={t.value}>
                      {t.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          )}
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="instituicao">Instituição</Label>
            <Input
              id="instituicao"
              name="instituicao"
              defaultValue={conta?.instituicao}
            />
          </div>
          {props.modo === "criar" && (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="saldoInicial">Saldo inicial</Label>
              <Input
                id="saldoInicial"
                name="saldoInicial"
                type="number"
                step="0.01"
                placeholder="0,00"
              />
            </div>
          )}
          {state.erro && (
            <p className="text-destructive text-sm">{state.erro}</p>
          )}
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

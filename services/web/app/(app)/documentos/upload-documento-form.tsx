"use client";

import { useActionState } from "react";
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
import type { Cartao } from "@/lib/card-service";
import { uploadDocumentoAction, type FormState } from "./actions";

const ESTADO_INICIAL: FormState = {};

export function UploadDocumentoForm({ cartoes }: { cartoes: Cartao[] }) {
  const [state, formAction, pending] = useActionState(
    uploadDocumentoAction,
    ESTADO_INICIAL
  );

  return (
    <form action={formAction} className="flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="arquivo">Fatura em PDF</Label>
        <Input id="arquivo" name="arquivo" type="file" accept="application/pdf" required />
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="cartaoId">Cartão</Label>
        <Select name="cartaoId" defaultValue={cartoes[0]?.id}>
          <SelectTrigger id="cartaoId" className="w-full max-w-sm">
            <SelectValue placeholder="Selecione o cartão" />
          </SelectTrigger>
          <SelectContent>
            {cartoes.map((cartao) => (
              <SelectItem key={cartao.id} value={cartao.id}>
                {cartao.apelido}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="flex flex-col gap-1.5">
        <Label htmlFor="senha">Senha do PDF (opcional)</Label>
        <Input id="senha" name="senha" type="password" />
      </div>
      {state.erro && <p className="text-destructive text-sm">{state.erro}</p>}
      <Button type="submit" disabled={pending || cartoes.length === 0} className="self-start">
        {pending ? "Enviando..." : "Enviar fatura"}
      </Button>
      {cartoes.length === 0 && (
        <p className="text-muted-foreground text-sm">
          Crie um cartão primeiro (aba Cartões) antes de enviar uma fatura.
        </p>
      )}
      <p className="text-muted-foreground text-xs">
        Se a fatura listar mais de uma pessoa (titular + adicional), só
        entram os lançamentos da sua seção — identificada automaticamente
        pelo seu nome de login. Compra nova (à vista ou parcelada) é
        lançada nesse cartão; compra já conhecida de um upload anterior não
        é lançada de novo. A extração roda em background e pode levar
        alguns minutos — você é levado pra tela de acompanhamento, com
        barra de progresso, assim que o upload for aceito.
      </p>
    </form>
  );
}

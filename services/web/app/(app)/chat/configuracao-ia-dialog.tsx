"use client";

import { useActionState, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
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
import type { ConfiguracaoIa } from "@/lib/ai-service";
import { definirConfiguracaoIaAction, type ConfiguracaoFormState } from "./actions";

const ESTADO_INICIAL: ConfiguracaoFormState = {};

export function ConfiguracaoIaDialog({
  trigger,
  configuracao,
}: {
  trigger: React.ReactNode;
  configuracao: ConfiguracaoIa;
}) {
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const [provedor, setProvedor] = useState(
    configuracao.provedor === "NENHUM" ? "OLLAMA" : configuracao.provedor
  );
  const [state, formAction, pending] = useActionState(
    definirConfiguracaoIaAction,
    ESTADO_INICIAL
  );
  const enviandoRef = useRef(false);

  useEffect(() => {
    if (enviandoRef.current && !pending && !state.erro) {
      setOpen(false);
      router.refresh();
    }
    enviandoRef.current = pending;
  }, [pending, state.erro, router]);

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<span>{trigger}</span>} />
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Configuração de IA</DialogTitle>
        </DialogHeader>
        <form action={formAction} className="flex flex-col gap-3">
          <div className="flex flex-col gap-1.5">
            <Label htmlFor="provedor">Provedor</Label>
            <Select
              name="provedor"
              value={provedor}
              onValueChange={(valor) => {
                if (valor) setProvedor(valor);
              }}
            >
              <SelectTrigger id="provedor" className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="OLLAMA">Ollama (local)</SelectItem>
                <SelectItem value="OPENAI">OpenAI</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {provedor === "OPENAI" ? (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="apiKey">API key da OpenAI</Label>
              <Input id="apiKey" name="apiKey" type="password" required />
            </div>
          ) : (
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="ollamaUrl">
                URL do Ollama (opcional — vazio usa o padrão local)
              </Label>
              <Input
                id="ollamaUrl"
                name="ollamaUrl"
                placeholder="http://localhost:11434"
                defaultValue={configuracao.ollamaUrl ?? ""}
              />
            </div>
          )}

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

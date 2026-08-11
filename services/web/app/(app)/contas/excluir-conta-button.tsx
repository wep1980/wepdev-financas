"use client";

import { ConfirmActionButton } from "@/components/confirm-action-button";
import { excluirContaAction } from "./actions";

export function ExcluirContaButton({ id, nome }: { id: string; nome: string }) {
  return (
    <ConfirmActionButton
      action={excluirContaAction}
      hiddenFields={{ id }}
      confirmMessage={`Excluir a conta "${nome}"? Isso não afeta o histórico já registrado.`}
    >
      Excluir
    </ConfirmActionButton>
  );
}

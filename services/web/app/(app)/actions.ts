"use server";

import { revalidatePath } from "next/cache";
import {
  BudgetServiceError,
  atualizarOrcamento,
  cancelarOrcamento,
  criarOrcamento,
  definirReserva,
} from "@/lib/budget-service";

export interface FormState {
  erro?: string;
}

function paraNumero(valor: FormDataEntryValue | null): number | null {
  const texto = String(valor ?? "").trim().replace(",", ".");
  if (!texto) return null;
  const numero = Number(texto);
  return Number.isNaN(numero) ? null : numero;
}

export async function criarOrcamentoAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const categoria = String(formData.get("categoria") ?? "").trim();
  const mesReferencia = String(formData.get("mesReferencia") ?? "").trim();
  const valorLimite = paraNumero(formData.get("valorLimite"));

  if (!categoria) return { erro: "Categoria é obrigatória" };
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(mesReferencia)) {
    return { erro: "Mês inválido" };
  }
  if (valorLimite === null || valorLimite <= 0) {
    return { erro: "Limite precisa ser maior que zero" };
  }

  try {
    await criarOrcamento({ categoria, mesReferencia, valorLimite });
  } catch (erro) {
    return {
      erro: erro instanceof BudgetServiceError ? erro.message : "Falha ao criar orçamento",
    };
  }

  revalidatePath("/");
  return {};
}

export async function atualizarOrcamentoAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  const valorLimite = paraNumero(formData.get("valorLimite"));

  if (!id) return { erro: "Orçamento inválido" };
  if (valorLimite === null || valorLimite <= 0) {
    return { erro: "Limite precisa ser maior que zero" };
  }

  try {
    await atualizarOrcamento(id, valorLimite);
  } catch (erro) {
    return {
      erro:
        erro instanceof BudgetServiceError ? erro.message : "Falha ao atualizar orçamento",
    };
  }

  revalidatePath("/");
  return {};
}

export async function cancelarOrcamentoAction(formData: FormData): Promise<void> {
  const id = String(formData.get("id") ?? "");
  if (!id) return;
  await cancelarOrcamento(id);
  revalidatePath("/");
}

export async function definirReservaAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const valor = paraNumero(formData.get("valor"));
  if (valor === null || valor < 0) {
    return { erro: "Valor precisa ser zero ou maior" };
  }

  try {
    await definirReserva(valor);
  } catch (erro) {
    return {
      erro: erro instanceof BudgetServiceError ? erro.message : "Falha ao definir reserva",
    };
  }

  revalidatePath("/");
  return {};
}

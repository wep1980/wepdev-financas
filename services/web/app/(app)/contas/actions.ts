"use server";

import { revalidatePath } from "next/cache";
import {
  AccountServiceError,
  atualizarConta,
  criarConta,
  excluirConta,
  type TipoConta,
} from "@/lib/account-service";

export interface FormState {
  erro?: string;
}

const TIPOS_VALIDOS: TipoConta[] = [
  "CORRENTE",
  "POUPANCA",
  "CARTEIRA",
  "CARTAO_CREDITO",
  "INVESTIMENTO",
];

export async function criarContaAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const nome = String(formData.get("nome") ?? "").trim();
  const tipo = String(formData.get("tipo") ?? "");
  const instituicao = String(formData.get("instituicao") ?? "").trim();
  const saldoInicialBruto = String(formData.get("saldoInicial") ?? "").trim();

  if (!nome) {
    return { erro: "Nome é obrigatório" };
  }
  if (!TIPOS_VALIDOS.includes(tipo as TipoConta)) {
    return { erro: "Tipo de conta inválido" };
  }

  const saldoInicial = saldoInicialBruto ? Number(saldoInicialBruto) : undefined;
  if (saldoInicial !== undefined && Number.isNaN(saldoInicial)) {
    return { erro: "Saldo inicial precisa ser um número" };
  }

  try {
    await criarConta({
      nome,
      tipo: tipo as TipoConta,
      instituicao: instituicao || undefined,
      saldoInicial,
    });
  } catch (erro) {
    return {
      erro: erro instanceof AccountServiceError ? erro.message : "Falha ao criar conta",
    };
  }

  revalidatePath("/contas");
  return {};
}

export async function atualizarContaAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  const nome = String(formData.get("nome") ?? "").trim();
  const instituicao = String(formData.get("instituicao") ?? "").trim();

  if (!id) {
    return { erro: "Conta inválida" };
  }
  if (!nome) {
    return { erro: "Nome é obrigatório" };
  }

  try {
    await atualizarConta(id, { nome, instituicao: instituicao || undefined });
  } catch (erro) {
    return {
      erro:
        erro instanceof AccountServiceError ? erro.message : "Falha ao atualizar conta",
    };
  }

  revalidatePath("/contas");
  return {};
}

export async function excluirContaAction(formData: FormData): Promise<void> {
  const id = String(formData.get("id") ?? "");
  if (!id) return;
  await excluirConta(id);
  revalidatePath("/contas");
}

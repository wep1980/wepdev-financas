"use server";

import { revalidatePath } from "next/cache";
import {
  TransactionServiceError,
  atualizarTransacao,
  cancelarTransacao,
  cancelarTransacaoRecorrente,
  criarTransacao,
  criarTransacaoRecorrente,
  type TipoTransacao,
} from "@/lib/transaction-service";

export interface FormState {
  erro?: string;
}

const TIPOS_VALIDOS: TipoTransacao[] = ["RECEITA", "DESPESA"];

function paraNumero(valor: FormDataEntryValue | null): number | null {
  const texto = String(valor ?? "").trim().replace(",", ".");
  if (!texto) return null;
  const numero = Number(texto);
  return Number.isNaN(numero) ? null : numero;
}

export async function criarTransacaoAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const contaId = String(formData.get("contaId") ?? "");
  const descricao = String(formData.get("descricao") ?? "").trim();
  const tipo = String(formData.get("tipo") ?? "");
  const categoria = String(formData.get("categoria") ?? "").trim();
  const dataTransacao = String(formData.get("dataTransacao") ?? "").trim();
  const valor = paraNumero(formData.get("valor"));

  if (!contaId) return { erro: "Selecione uma conta" };
  if (!descricao) return { erro: "Descrição é obrigatória" };
  if (!TIPOS_VALIDOS.includes(tipo as TipoTransacao)) {
    return { erro: "Tipo inválido" };
  }
  if (valor === null || valor <= 0) return { erro: "Valor precisa ser maior que zero" };

  try {
    await criarTransacao({
      contaId,
      descricao,
      valor,
      tipo: tipo as TipoTransacao,
      categoria: categoria || undefined,
      dataTransacao: dataTransacao || undefined,
    });
  } catch (erro) {
    return {
      erro:
        erro instanceof TransactionServiceError ? erro.message : "Falha ao registrar transação",
    };
  }

  revalidatePath("/transacoes");
  return {};
}

export async function atualizarTransacaoAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const id = String(formData.get("id") ?? "");
  const descricao = String(formData.get("descricao") ?? "").trim();
  const categoria = String(formData.get("categoria") ?? "").trim();
  const dataTransacao = String(formData.get("dataTransacao") ?? "").trim();
  const valor = paraNumero(formData.get("valor"));

  if (!id) return { erro: "Transação inválida" };
  if (!descricao) return { erro: "Descrição é obrigatória" };
  if (valor === null || valor <= 0) return { erro: "Valor precisa ser maior que zero" };

  try {
    await atualizarTransacao(id, {
      descricao,
      valor,
      categoria: categoria || undefined,
      dataTransacao: dataTransacao || undefined,
    });
  } catch (erro) {
    return {
      erro:
        erro instanceof TransactionServiceError ? erro.message : "Falha ao atualizar transação",
    };
  }

  revalidatePath("/transacoes");
  return {};
}

export async function cancelarTransacaoAction(formData: FormData): Promise<void> {
  const id = String(formData.get("id") ?? "");
  if (!id) return;
  await cancelarTransacao(id);
  revalidatePath("/transacoes");
}

export async function criarTransacaoRecorrenteAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const contaId = String(formData.get("contaId") ?? "");
  const descricao = String(formData.get("descricao") ?? "").trim();
  const tipo = String(formData.get("tipo") ?? "");
  const categoria = String(formData.get("categoria") ?? "").trim();
  const dataInicio = String(formData.get("dataInicio") ?? "").trim();
  const quantidadeOcorrenciasBruta = String(
    formData.get("quantidadeOcorrencias") ?? ""
  ).trim();
  const valor = paraNumero(formData.get("valor"));

  if (!contaId) return { erro: "Selecione uma conta" };
  if (!descricao) return { erro: "Descrição é obrigatória" };
  if (!TIPOS_VALIDOS.includes(tipo as TipoTransacao)) {
    return { erro: "Tipo inválido" };
  }
  if (valor === null || valor <= 0) return { erro: "Valor precisa ser maior que zero" };
  if (!dataInicio) return { erro: "Data de início é obrigatória" };

  const quantidadeOcorrencias = quantidadeOcorrenciasBruta
    ? Number(quantidadeOcorrenciasBruta)
    : undefined;
  if (quantidadeOcorrencias !== undefined && Number.isNaN(quantidadeOcorrencias)) {
    return { erro: "Quantidade de ocorrências precisa ser um número" };
  }

  try {
    await criarTransacaoRecorrente({
      contaId,
      descricao,
      valor,
      tipo: tipo as TipoTransacao,
      categoria: categoria || undefined,
      frequencia: "MENSAL",
      dataInicio,
      quantidadeOcorrencias,
    });
  } catch (erro) {
    return {
      erro:
        erro instanceof TransactionServiceError
          ? erro.message
          : "Falha ao criar regra recorrente",
    };
  }

  revalidatePath("/transacoes");
  return {};
}

export async function cancelarTransacaoRecorrenteAction(formData: FormData): Promise<void> {
  const id = String(formData.get("id") ?? "");
  if (!id) return;
  await cancelarTransacaoRecorrente(id);
  revalidatePath("/transacoes");
}

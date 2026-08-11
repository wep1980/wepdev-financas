"use server";

import { revalidatePath } from "next/cache";
import { CardServiceError, criarCartao, type Bandeira } from "@/lib/card-service";

export interface FormState {
  erro?: string;
}

const BANDEIRAS_VALIDAS: Bandeira[] = ["VISA", "MASTERCARD", "ELO", "AMEX", "OUTRA"];

export async function criarCartaoAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const apelido = String(formData.get("apelido") ?? "").trim();
  const bandeira = String(formData.get("bandeira") ?? "");
  const limiteBruto = String(formData.get("limite") ?? "").trim();
  const diaFechamentoBruto = String(formData.get("diaFechamento") ?? "").trim();
  const diaVencimentoBruto = String(formData.get("diaVencimento") ?? "").trim();
  const contaPagamentoId = String(formData.get("contaPagamentoId") ?? "");

  if (!apelido) {
    return { erro: "Apelido é obrigatório" };
  }
  if (!BANDEIRAS_VALIDAS.includes(bandeira as Bandeira)) {
    return { erro: "Bandeira inválida" };
  }
  if (!contaPagamentoId) {
    return { erro: "Selecione a conta que vai pagar a fatura" };
  }

  const limite = Number(limiteBruto);
  const diaFechamento = Number(diaFechamentoBruto);
  const diaVencimento = Number(diaVencimentoBruto);
  if (!limiteBruto || Number.isNaN(limite) || limite <= 0) {
    return { erro: "Limite precisa ser um número positivo" };
  }
  if (!diaFechamentoBruto || Number.isNaN(diaFechamento) || diaFechamento < 1 || diaFechamento > 31) {
    return { erro: "Dia de fechamento precisa estar entre 1 e 31" };
  }
  if (!diaVencimentoBruto || Number.isNaN(diaVencimento) || diaVencimento < 1 || diaVencimento > 31) {
    return { erro: "Dia de vencimento precisa estar entre 1 e 31" };
  }

  try {
    await criarCartao({
      apelido,
      bandeira: bandeira as Bandeira,
      limite,
      diaFechamento,
      diaVencimento,
      contaPagamentoId,
    });
  } catch (erro) {
    return {
      erro: erro instanceof CardServiceError ? erro.message : "Falha ao criar cartão",
    };
  }

  revalidatePath("/cartoes");
  revalidatePath("/documentos");
  return {};
}

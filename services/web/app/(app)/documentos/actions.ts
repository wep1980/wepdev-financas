"use server";

import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import {
  DocumentServiceError,
  buscarDocumento,
  confirmarLancamentos,
  uploadDocumento,
  type DocumentoImportado,
} from "@/lib/document-service";

export interface FormState {
  erro?: string;
}

export async function uploadDocumentoAction(
  _estadoAnterior: FormState,
  formData: FormData
): Promise<FormState> {
  const arquivo = formData.get("arquivo");
  const cartaoId = String(formData.get("cartaoId") ?? "");
  const senha = String(formData.get("senha") ?? "").trim();

  if (!(arquivo instanceof File) || arquivo.size === 0) {
    return { erro: "Selecione um arquivo PDF" };
  }
  if (!cartaoId) {
    return { erro: "Selecione o cartão dessa fatura" };
  }

  let documento: DocumentoImportado;
  try {
    documento = await uploadDocumento(arquivo, "FATURA_CARTAO", cartaoId, senha || undefined);
  } catch (erro) {
    return {
      erro: erro instanceof DocumentServiceError ? erro.message : "Falha ao enviar documento",
    };
  }

  revalidatePath("/documentos");
  redirect(`/documentos/${documento.id}`);
}

/** Chamada direta (não via <form>) pelo poller client-side enquanto o
 * documento ainda está RECEBIDO/PROCESSANDO — extração em background
 * pode levar minutos (ver document-service.yaml). */
export async function buscarDocumentoAction(id: string): Promise<DocumentoImportado> {
  return buscarDocumento(id);
}

export interface ConfirmarFormState {
  erro?: string;
}

export async function confirmarLancamentosAction(
  _estadoAnterior: ConfirmarFormState,
  formData: FormData
): Promise<ConfirmarFormState> {
  const documentoId = String(formData.get("documentoId") ?? "");
  const lancamentoIdsConfirmados = formData.getAll("lancamentoIdsConfirmados").map(String);

  if (!documentoId) return { erro: "Documento inválido" };
  if (lancamentoIdsConfirmados.length === 0) {
    return { erro: "Selecione ao menos um lançamento pra confirmar" };
  }

  try {
    await confirmarLancamentos(documentoId, lancamentoIdsConfirmados);
  } catch (erro) {
    return {
      erro:
        erro instanceof DocumentServiceError ? erro.message : "Falha ao confirmar lançamentos",
    };
  }

  revalidatePath(`/documentos/${documentoId}`);
  revalidatePath("/documentos");
  return {};
}

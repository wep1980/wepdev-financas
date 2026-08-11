import "server-only";

export interface CotacaoDolar {
  compra: number;
  venda: number;
  dataHora: string;
}

const URL_AWESOMEAPI = "https://economia.awesomeapi.com.br/last/USD-BRL";

/**
 * Cotação USD/BRL via AwesomeAPI (decisão do usuário, 2026-08-10) — API
 * pública brasileira, sem autenticação/chave. Dado público, não sensível
 * e igual pra todo usuário, então cacheia por 5min em vez do
 * `cache: "no-store"` usado nos clients de microsserviço (account/
 * transaction/budget/document-service) — aqueles são por usuário e
 * nunca podem vazar entre requests, este aqui pode e deve ser
 * compartilhado.
 */
export async function buscarCotacaoDolar(): Promise<CotacaoDolar | null> {
  try {
    const resposta = await fetch(URL_AWESOMEAPI, {
      next: { revalidate: 300 },
    });
    if (!resposta.ok) return null;

    const corpo = await resposta.json();
    const dados = corpo.USDBRL;
    if (!dados) return null;

    return {
      compra: Number(dados.bid),
      venda: Number(dados.ask),
      dataHora: dados.create_date,
    };
  } catch {
    // Cotação é informativa, não crítica — indisponibilidade da
    // AwesomeAPI não pode derrubar o dashboard inteiro.
    return null;
  }
}

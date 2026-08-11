/** Formato AAAA-MM usado em toda a API do budget-service/transaction-service. */
export function mesAtual(): string {
  const agora = new Date();
  return `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, "0")}`;
}

export function limitesDoMes(mes: string): { inicio: string; fim: string } {
  const [ano, mesNumero] = mes.split("-").map(Number);
  const inicio = `${mes}-01`;
  // Dia 0 do mês seguinte = último dia deste mês (comportamento do Date do JS).
  const ultimoDia = new Date(ano, mesNumero, 0).getDate();
  const fim = `${mes}-${String(ultimoDia).padStart(2, "0")}`;
  return { inicio, fim };
}

/**
 * Janela de N meses terminando em mesReferencia (inclusive) — usada pra
 * sugestão de reserva (média de receita dos últimos meses). new Date com
 * monthIndex negativo já rola o ano pra trás sozinho (comportamento
 * padrão do JS), então funciona sem tratamento especial virando o ano.
 */
export function limitesUltimosMeses(
  mesReferencia: string,
  quantidadeMeses: number
): { inicio: string; fim: string } {
  const [ano, mesNumero] = mesReferencia.split("-").map(Number);
  const dataInicio = new Date(ano, mesNumero - 1 - (quantidadeMeses - 1), 1);
  const inicio = `${dataInicio.getFullYear()}-${String(dataInicio.getMonth() + 1).padStart(2, "0")}-01`;
  const { fim } = limitesDoMes(mesReferencia);
  return { inicio, fim };
}

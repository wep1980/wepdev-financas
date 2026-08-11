import { buscarDisponivelParaGastar, buscarReserva, listarOrcamentos } from "@/lib/budget-service";
import { listarTransacoes, resumoPorCategoria } from "@/lib/transaction-service";
import { limitesDoMes, limitesUltimosMeses, mesAtual } from "@/lib/mes";
import { MESES_MEDIA_RECEITA, calcularReservaSugerida } from "@/lib/reserva-sugerida";
import { buscarCotacaoDolar } from "@/lib/cambio";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConfirmActionButton } from "@/components/confirm-action-button";
import { GastosPorCategoriaChart } from "./gastos-por-categoria-chart";
import { OrcamentoFormDialog } from "./orcamento-form-dialog";
import { ReservaForm } from "./reserva-form";
import { cancelarOrcamentoAction } from "./actions";

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

interface PageProps {
  searchParams: Promise<{ mes?: string }>;
}

export default async function DashboardPage({ searchParams }: PageProps) {
  const { mes: mesParam } = await searchParams;
  const mes = mesParam ?? mesAtual();
  const { inicio, fim } = limitesDoMes(mes);
  const janelaReceita = limitesUltimosMeses(mes, MESES_MEDIA_RECEITA);

  const [disponivel, orcamentos, reserva, resumo, transacoesRecentes, cotacaoDolar] =
    await Promise.all([
      buscarDisponivelParaGastar(mes),
      listarOrcamentos(mes),
      buscarReserva(),
      resumoPorCategoria(inicio, fim),
      listarTransacoes(janelaReceita),
      buscarCotacaoDolar(),
    ]);

  const reservaSugerida = calcularReservaSugerida(transacoesRecentes);

  return (
    <div className="flex flex-col gap-8 p-6 md:p-8">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold tracking-tight">Dashboard</h1>
        <form method="GET" className="flex items-center gap-2">
          <input
            type="month"
            name="mes"
            defaultValue={mes}
            className="border-input h-8 rounded-lg border bg-transparent px-2.5 text-sm"
          />
          <Button type="submit" variant="outline" size="sm">
            Ver
          </Button>
        </form>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Disponível pra gastar</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            <span
              className={`text-3xl font-semibold tabular-nums ${
                disponivel.valorDisponivel < 0 ? "text-destructive" : "text-primary"
              }`}
            >
              {FORMATADOR_MOEDA.format(disponivel.valorDisponivel)}
            </span>
            <dl className="text-muted-foreground grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
              <dt>Saldo em contas</dt>
              <dd className="text-right tabular-nums">
                {FORMATADOR_MOEDA.format(disponivel.saldoContas)}
              </dd>
              <dt>Faturas em aberto</dt>
              <dd className="text-right tabular-nums">
                -{FORMATADOR_MOEDA.format(disponivel.faturasEmAberto)}
              </dd>
              <dt>Despesas recorrentes</dt>
              <dd className="text-right tabular-nums">
                -{FORMATADOR_MOEDA.format(disponivel.despesasRecorrentes)}
              </dd>
              <dt>Reserva</dt>
              <dd className="text-right tabular-nums">
                -{FORMATADOR_MOEDA.format(disponivel.reserva)}
              </dd>
            </dl>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Reserva</CardTitle>
          </CardHeader>
          <CardContent>
            <ReservaForm valorAtual={reserva.valor} sugestao={reservaSugerida} />
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Dólar hoje</CardTitle>
        </CardHeader>
        <CardContent>
          {cotacaoDolar ? (
            <div className="flex flex-wrap items-baseline gap-x-6 gap-y-1">
              <div className="flex flex-col">
                <span className="text-2xl font-semibold tabular-nums">
                  {FORMATADOR_MOEDA.format(cotacaoDolar.venda)}
                </span>
                <span className="text-muted-foreground text-xs">
                  pra comprar US$ 1 (venda)
                </span>
              </div>
              <div className="flex flex-col">
                <span className="font-medium tabular-nums">
                  {FORMATADOR_MOEDA.format(cotacaoDolar.compra)}
                </span>
                <span className="text-muted-foreground text-xs">compra</span>
              </div>
              <span className="text-muted-foreground text-xs">
                Atualizado {cotacaoDolar.dataHora}
              </span>
            </div>
          ) : (
            <p className="text-muted-foreground text-sm">
              Cotação indisponível no momento.
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Gastos por categoria</CardTitle>
        </CardHeader>
        <CardContent>
          <GastosPorCategoriaChart dados={resumo} />
        </CardContent>
      </Card>

      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold tracking-tight">Orçamentos</h2>
          <OrcamentoFormDialog
            modo="criar"
            mes={mes}
            trigger={<Button>Novo orçamento</Button>}
          />
        </div>

        {orcamentos.length === 0 ? (
          <p className="text-muted-foreground">
            Nenhum orçamento definido pra esse mês ainda.
          </p>
        ) : (
          <div className="divide-border border-border divide-y rounded-lg border">
            {orcamentos.map((orcamento) => (
              <div
                key={orcamento.id}
                className="flex items-center justify-between gap-4 px-4 py-3"
              >
                <div className="flex flex-col">
                  <span className="font-medium">{orcamento.categoria}</span>
                  <span className="text-muted-foreground text-sm">
                    {FORMATADOR_MOEDA.format(orcamento.valorConsumido)} de{" "}
                    {FORMATADOR_MOEDA.format(orcamento.valorLimite)} (
                    {orcamento.percentualConsumido.toFixed(0)}%)
                  </span>
                </div>
                <div className="flex items-center gap-4">
                  <span
                    className={`font-medium tabular-nums ${
                      orcamento.valorDisponivel < 0 ? "text-destructive" : ""
                    }`}
                  >
                    {FORMATADOR_MOEDA.format(orcamento.valorDisponivel)}
                  </span>
                  <OrcamentoFormDialog
                    modo="editar"
                    orcamento={orcamento}
                    trigger={
                      <Button variant="outline" size="sm">
                        Editar
                      </Button>
                    }
                  />
                  <ConfirmActionButton
                    action={cancelarOrcamentoAction}
                    hiddenFields={{ id: orcamento.id }}
                    confirmMessage={`Cancelar o orçamento de "${orcamento.categoria}"?`}
                  >
                    Cancelar
                  </ConfirmActionButton>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

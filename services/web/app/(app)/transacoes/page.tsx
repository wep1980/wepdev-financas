import { listarContas } from "@/lib/account-service";
import { listarTransacoes, listarTransacoesRecorrentes } from "@/lib/transaction-service";
import { Button } from "@/components/ui/button";
import { ConfirmActionButton } from "@/components/confirm-action-button";
import { TransacaoFormDialog } from "./transacao-form-dialog";
import { RecorrenteFormDialog } from "./recorrente-form-dialog";
import {
  cancelarTransacaoAction,
  cancelarTransacaoRecorrenteAction,
} from "./actions";

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const STATUS_LABEL: Record<string, string> = {
  PENDENTE: "Pendente",
  CONFIRMADA: "Confirmada",
  CANCELADA: "Cancelada",
  ATIVA: "Ativa",
  PAUSADA: "Pausada",
  CONCLUIDA: "Concluída",
};

interface PageProps {
  searchParams: Promise<{ contaId?: string; inicio?: string; fim?: string }>;
}

export default async function TransacoesPage({ searchParams }: PageProps) {
  const filtro = await searchParams;

  const [contas, transacoes, recorrentes] = await Promise.all([
    listarContas(),
    listarTransacoes(filtro),
    listarTransacoesRecorrentes(),
  ]);

  return (
    <div className="flex flex-col gap-10 p-6 md:p-8">
      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h1 className="text-xl font-semibold tracking-tight">Transações</h1>
          {contas.length > 0 && (
            <TransacaoFormDialog
              modo="criar"
              contas={contas}
              trigger={<Button>Nova transação</Button>}
            />
          )}
        </div>

        <form className="flex flex-wrap items-end gap-3 text-sm" method="GET">
          <div className="flex flex-col gap-1">
            <label htmlFor="contaId" className="text-muted-foreground text-xs">
              Conta
            </label>
            <select
              id="contaId"
              name="contaId"
              defaultValue={filtro.contaId ?? ""}
              className="border-input h-8 rounded-lg border bg-transparent px-2.5 text-sm"
            >
              <option value="">Todas</option>
              {contas.map((conta) => (
                <option key={conta.id} value={conta.id}>
                  {conta.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="inicio" className="text-muted-foreground text-xs">
              De
            </label>
            <input
              id="inicio"
              name="inicio"
              type="date"
              defaultValue={filtro.inicio ?? ""}
              className="border-input h-8 rounded-lg border bg-transparent px-2.5 text-sm"
            />
          </div>
          <div className="flex flex-col gap-1">
            <label htmlFor="fim" className="text-muted-foreground text-xs">
              Até
            </label>
            <input
              id="fim"
              name="fim"
              type="date"
              defaultValue={filtro.fim ?? ""}
              className="border-input h-8 rounded-lg border bg-transparent px-2.5 text-sm"
            />
          </div>
          <Button type="submit" variant="outline" size="sm">
            Filtrar
          </Button>
        </form>

        {contas.length === 0 ? (
          <p className="text-muted-foreground">
            Crie uma conta primeiro (aba Contas) antes de registrar transações.
          </p>
        ) : transacoes.length === 0 ? (
          <p className="text-muted-foreground">
            Nenhuma transação encontrada com esse filtro.
          </p>
        ) : (
          <div className="divide-border border-border divide-y rounded-lg border">
            {transacoes.map((transacao) => (
              <div
                key={transacao.id}
                className="flex items-center justify-between gap-4 px-4 py-3"
              >
                <div className="flex flex-col">
                  <span className="font-medium">{transacao.descricao}</span>
                  <span className="text-muted-foreground text-sm">
                    {transacao.categoria ?? "Sem categoria"} ·{" "}
                    {transacao.dataTransacao} ·{" "}
                    {STATUS_LABEL[transacao.status] ?? transacao.status}
                  </span>
                </div>
                <div className="flex items-center gap-4">
                  <span
                    className={`font-medium tabular-nums ${
                      transacao.tipo === "DESPESA"
                        ? "text-destructive"
                        : "text-primary"
                    }`}
                  >
                    {transacao.tipo === "DESPESA" ? "-" : "+"}
                    {FORMATADOR_MOEDA.format(transacao.valor)}
                  </span>
                  {transacao.status !== "CANCELADA" && (
                    <>
                      <TransacaoFormDialog
                        modo="editar"
                        transacao={transacao}
                        trigger={
                          <Button variant="outline" size="sm">
                            Editar
                          </Button>
                        }
                      />
                      <ConfirmActionButton
                        action={cancelarTransacaoAction}
                        hiddenFields={{ id: transacao.id }}
                        confirmMessage={`Cancelar "${transacao.descricao}"? O saldo é revertido.`}
                      >
                        Cancelar
                      </ConfirmActionButton>
                    </>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold tracking-tight">
            Transações recorrentes
          </h2>
          {contas.length > 0 && (
            <RecorrenteFormDialog
              contas={contas}
              trigger={<Button variant="outline">Nova recorrente</Button>}
            />
          )}
        </div>

        {recorrentes.length === 0 ? (
          <p className="text-muted-foreground">Nenhuma regra recorrente ainda.</p>
        ) : (
          <div className="divide-border border-border divide-y rounded-lg border">
            {recorrentes.map((regra) => (
              <div
                key={regra.id}
                className="flex items-center justify-between gap-4 px-4 py-3"
              >
                <div className="flex flex-col">
                  <span className="font-medium">{regra.descricao}</span>
                  <span className="text-muted-foreground text-sm">
                    Mensal · desde {regra.dataInicio} · {regra.ocorrenciasGeradas}{" "}
                    ocorrência(s) geradas ·{" "}
                    {STATUS_LABEL[regra.status] ?? regra.status}
                  </span>
                </div>
                <div className="flex items-center gap-4">
                  <span
                    className={`font-medium tabular-nums ${
                      regra.tipo === "DESPESA" ? "text-destructive" : "text-primary"
                    }`}
                  >
                    {regra.tipo === "DESPESA" ? "-" : "+"}
                    {FORMATADOR_MOEDA.format(regra.valor)}
                  </span>
                  {regra.status === "ATIVA" && (
                    <ConfirmActionButton
                      action={cancelarTransacaoRecorrenteAction}
                      hiddenFields={{ id: regra.id }}
                      confirmMessage={`Cancelar a regra "${regra.descricao}"? Ocorrências já geradas não são afetadas.`}
                    >
                      Cancelar
                    </ConfirmActionButton>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

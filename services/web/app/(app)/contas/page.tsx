import { listarContas } from "@/lib/account-service";
import { Button } from "@/components/ui/button";
import { ContaFormDialog } from "./conta-form-dialog";
import { ExcluirContaButton } from "./excluir-conta-button";

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const TIPO_LABEL: Record<string, string> = {
  CORRENTE: "Conta corrente",
  POUPANCA: "Poupança",
  CARTEIRA: "Carteira",
  CARTAO_CREDITO: "Cartão de crédito",
  INVESTIMENTO: "Investimento",
};

export default async function ContasPage() {
  const contas = await listarContas();

  return (
    <div className="flex flex-col gap-6 p-6 md:p-8">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold tracking-tight">Contas</h1>
        <ContaFormDialog modo="criar" trigger={<Button>Nova conta</Button>} />
      </div>

      {contas.length === 0 ? (
        <p className="text-muted-foreground">
          Nenhuma conta ainda — crie a primeira com o botão acima.
        </p>
      ) : (
        <div className="divide-border border-border divide-y rounded-lg border">
          {contas.map((conta) => (
            <div
              key={conta.id}
              className="flex items-center justify-between gap-4 px-4 py-3"
            >
              <div className="flex flex-col">
                <span className="font-medium">{conta.nome}</span>
                <span className="text-muted-foreground text-sm">
                  {TIPO_LABEL[conta.tipo] ?? conta.tipo}
                  {conta.instituicao ? ` · ${conta.instituicao}` : ""}
                </span>
              </div>
              <div className="flex items-center gap-4">
                <span className="font-medium tabular-nums">
                  {FORMATADOR_MOEDA.format(conta.saldo)}
                </span>
                <ContaFormDialog
                  modo="editar"
                  conta={conta}
                  trigger={
                    <Button variant="outline" size="sm">
                      Editar
                    </Button>
                  }
                />
                <ExcluirContaButton id={conta.id} nome={conta.nome} />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

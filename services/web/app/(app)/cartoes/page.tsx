import { listarContas } from "@/lib/account-service";
import { listarCartoes } from "@/lib/card-service";
import { Button } from "@/components/ui/button";
import { CartaoFormDialog } from "./cartao-form-dialog";

const FORMATADOR_MOEDA = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

const BANDEIRA_LABEL: Record<string, string> = {
  VISA: "Visa",
  MASTERCARD: "Mastercard",
  ELO: "Elo",
  AMEX: "Amex",
  OUTRA: "Outra",
};

export default async function CartoesPage() {
  const [cartoes, contas] = await Promise.all([listarCartoes(), listarContas()]);
  const contaPorId = new Map(contas.map((conta) => [conta.id, conta.nome]));

  return (
    <div className="flex flex-col gap-6 p-6 md:p-8">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold tracking-tight">Cartões</h1>
        <CartaoFormDialog trigger={<Button>Novo cartão</Button>} contas={contas} />
      </div>

      {cartoes.length === 0 ? (
        <p className="text-muted-foreground">
          Nenhum cartão ainda — crie o primeiro com o botão acima. É
          necessário pra fazer upload de fatura (a fatura sempre pertence a
          um cartão cadastrado).
        </p>
      ) : (
        <div className="divide-border border-border divide-y rounded-lg border">
          {cartoes.map((cartao) => (
            <div
              key={cartao.id}
              className="flex items-center justify-between gap-4 px-4 py-3"
            >
              <div className="flex flex-col">
                <span className="font-medium">{cartao.apelido}</span>
                <span className="text-muted-foreground text-sm">
                  {BANDEIRA_LABEL[cartao.bandeira] ?? cartao.bandeira} · fecha
                  dia {cartao.diaFechamento}, vence dia {cartao.diaVencimento}{" "}
                  · paga com {contaPorId.get(cartao.contaPagamentoId) ?? "conta removida"}
                </span>
              </div>
              <span className="font-medium tabular-nums">
                {FORMATADOR_MOEDA.format(cartao.limite)}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

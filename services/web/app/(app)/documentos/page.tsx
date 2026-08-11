import Link from "next/link";
import { listarDocumentos } from "@/lib/document-service";
import { listarCartoes } from "@/lib/card-service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { UploadDocumentoForm } from "./upload-documento-form";

const STATUS_LABEL: Record<string, string> = {
  RECEBIDO: "Recebido, aguardando processamento",
  PROCESSANDO: "Processando",
  AGUARDANDO_CONFIRMACAO: "Aguardando confirmação",
  CONFIRMADO: "Confirmado",
  ERRO_PROCESSAMENTO: "Erro no processamento",
};

const STATUS_COR: Record<string, string> = {
  RECEBIDO: "text-muted-foreground",
  PROCESSANDO: "text-muted-foreground",
  AGUARDANDO_CONFIRMACAO: "text-primary",
  CONFIRMADO: "text-primary",
  ERRO_PROCESSAMENTO: "text-destructive",
};

const FORMATADOR_DATA = new Intl.DateTimeFormat("pt-BR");

export default async function DocumentosPage() {
  const [documentos, cartoes] = await Promise.all([listarDocumentos(), listarCartoes()]);

  return (
    <div className="flex flex-col gap-6 p-6 md:p-8">
      <h1 className="text-xl font-semibold tracking-tight">Documentos</h1>

      <Card>
        <CardHeader>
          <CardTitle>Importar fatura de cartão</CardTitle>
        </CardHeader>
        <CardContent>
          <UploadDocumentoForm cartoes={cartoes} />
        </CardContent>
      </Card>

      <section className="flex flex-col gap-4">
        <h2 className="text-lg font-semibold tracking-tight">
          Documentos importados
        </h2>
        {documentos.length === 0 ? (
          <p className="text-muted-foreground">Nenhum documento importado ainda.</p>
        ) : (
          <div className="divide-border border-border divide-y rounded-lg border">
            {documentos.map((documento) => (
              <Link
                key={documento.id}
                href={`/documentos/${documento.id}`}
                className="hover:bg-muted/50 flex items-center justify-between gap-4 px-4 py-3 transition-colors"
              >
                <div className="flex flex-col">
                  <span className="font-medium">{documento.nomeArquivo}</span>
                  <span className="text-muted-foreground text-sm">
                    Enviado em {FORMATADOR_DATA.format(new Date(documento.criadoEm))}
                  </span>
                </div>
                <span
                  className={`text-sm font-medium ${STATUS_COR[documento.status] ?? ""}`}
                >
                  {STATUS_LABEL[documento.status] ?? documento.status}
                </span>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

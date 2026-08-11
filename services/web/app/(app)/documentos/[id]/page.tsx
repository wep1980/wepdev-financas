import { buscarDocumento } from "@/lib/document-service";
import { DocumentoDetalhe } from "./documento-detalhe";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function DocumentoDetalhePage({ params }: PageProps) {
  const { id } = await params;
  const documento = await buscarDocumento(id);

  return (
    <div className="flex flex-col gap-6 p-6 md:p-8">
      <h1 className="text-xl font-semibold tracking-tight">
        {documento.nomeArquivo}
      </h1>
      <DocumentoDetalhe documentoInicial={documento} />
    </div>
  );
}

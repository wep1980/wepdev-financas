import { buscarConfiguracaoIa, buscarConversa } from "@/lib/ai-service";
import { ChatClient } from "../chat-client";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default async function ConversaPage({ params }: PageProps) {
  const { id } = await params;
  const [conversa, configuracao] = await Promise.all([
    buscarConversa(id),
    buscarConfiguracaoIa(),
  ]);

  return (
    <ChatClient
      conversaId={conversa.id}
      mensagensIniciais={conversa.mensagens}
      iaConfigurada={configuracao.configurado}
    />
  );
}

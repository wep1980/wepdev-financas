import { buscarConfiguracaoIa } from "@/lib/ai-service";
import { ChatClient } from "./chat-client";

export default async function NovaConversaPage() {
  const configuracao = await buscarConfiguracaoIa();

  return (
    <ChatClient
      mensagensIniciais={[]}
      iaConfigurada={configuracao.configurado}
    />
  );
}

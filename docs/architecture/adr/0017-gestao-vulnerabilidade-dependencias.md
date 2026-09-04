# ADR-0017: Gestão de vulnerabilidade de dependências

Status: Aceita
Data: 2026-08-06

## Contexto

O usuário pediu explicitamente pra evitar bibliotecas com vulnerabilidade
conhecida. Isso não é uma escolha pontual ("verificar antes de adicionar
uma lib nova") — sem processo automatizado, uma dependência que era segura
no dia em que foi adicionada pode ganhar uma CVE nova a qualquer momento, e
ninguém percebe até ser tarde. O projeto já usa GitHub Actions (CI/CD,
`README.md`) e tem dependências em três ecossistemas: Maven (serviços Java/
Quarkus), npm (Next.js web, React Native mobile) e imagens Docker (bancos,
Kafka, Keycloak, etc. no `docker-compose.yml`).

## Decisão

1. **GitHub Dependabot** habilitado (`.github/dependabot.yml`, a criar
   quando o repositório for inicializado) pra Maven, npm (um bloco por
   `package.json` — web, mobile, e cada front que existir) e Docker —
   monitora vulnerabilidade conhecida e abre PR de atualização
   automaticamente. Zero serviço externo, nativo do GitHub, sem custo.
2. **Gate no CI** (GitHub Actions), falha o build se houver vulnerabilidade
   HIGH/CRITICAL sem exceção documentada:
   - Java: `Trivy` sobre a imagem Docker construída no próprio job, cobrindo
     bibliotecas Java e pacotes do sistema operacional sem API key da NVD.
   - Node (web/mobile): `npm audit --audit-level=high`.
   - Imagens Docker: o mesmo `Trivy` inclui o SO da imagem base, não só a
     aplicação.
3. **Processo de exceção**: vulnerabilidade sem correção disponível ainda
   (ou falso positivo) não trava o CI pra sempre — precisa de justificativa
   documentada (arquivo de supressão com motivo + data de revisão), nunca
   suprimida silenciosamente. Ver `docs/architecture/security.md` seção 7.

## Consequências

- Toda dependência nova (lib Java, pacote npm) passa pelo mesmo scan antes
  de entrar — não precisa de checagem manual separada, o CI já barra.
- PRs de atualização de dependência do Dependabot ainda passam pela mesma
  suíte de testes (`testing-strategy.md`) antes de merge — atualização
  automática não é merge automático.
- **Isso cobre vulnerabilidade catalogada (CVE), não risco estrutural de lib
  não-oficial/não-catalogada** — o caso do WhatsApp via Baileys (ADR-0012) é
  um risco diferente (protocolo reverso, sem garantia de manutenção), que
  scan de CVE não detecta. As duas coisas são geridas separadamente: esta
  ADR pra CVE conhecida, ADR-0012 pro risco já assumido conscientemente ali.
- Mais um workflow de CI a manter; aceito porque o custo de detectar tarde
  (dado financeiro exposto por uma lib vulnerável) é desproporcionalmente
  maior.

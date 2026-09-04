# ADR-0030: Deploy em Kubernetes com Argo CD e imagens no GHCR

Status: Aceita — supera ADR-0020 e ADR-0021 e refina ADR-0016/0018
Data: 2026-09-04

## Contexto

As ADRs 0020 e 0021 escolheram runner self-hosted e Kamal quando o servidor
ainda não possuía uma plataforma Kubernetes. Depois dessa decisão, o servidor
foi ampliado e passou a operar K3s, Traefik e Argo CD. Também foi criado o
repositório privado `wep1980/servidor-gitops`, acessível pelo Argo CD por uma
Deploy Key somente leitura.

Manter dois mecanismos de implantação para os mesmos serviços aumentaria a
complexidade operacional e permitiria divergência entre o estado declarado e
o estado real. O novo ambiente torna possível adotar GitOps sem abrir SSH para
os runners do GitHub e sem executar código de CI dentro do host de produção.

## Decisão

- O CI continua em runners hospedados do GitHub e preserva os testes e gates de
  vulnerabilidade existentes.
- Em pushes na `main`, somente os serviços alterados publicam imagens em
  `ghcr.io/wep1980`, com a tag imutável igual ao SHA completo do commit e a tag
  móvel `main` para inspeção humana. Os manifests de produção sempre usam SHA
  ou digest, nunca `latest` nem `main`.
- O repositório da aplicação não possui acesso direto ao cluster. Ele propõe a
  atualização das referências de imagem no repositório GitOps por pull request.
- O Argo CD mantém acesso somente leitura ao Git e sincroniza o estado aprovado
  da branch `main` do repositório GitOps.
- O K3s/Traefik executa e roteia os serviços stateless. Cloudflare Tunnel
  continua sendo a entrada pública, agora apontando para o Ingress do Traefik.
- Bancos, mensageria, Keycloak, Qdrant e segredos só serão migrados depois de
  definidos armazenamento, backup, recursos e gestão de segredos. O pipeline
  de imagens não autoriza implantação incompleta desses componentes.

## Consequências

- Não é necessário runner self-hosted nem Kamal para este projeto. ADR-0020 e
  ADR-0021 ficam superadas, mas preservadas como histórico.
- Produção não recebe credenciais de cluster no GitHub Actions.
- A atualização entre repositórios exige uma GitHub App ou credencial de escopo
  mínimo capaz de criar branch e pull request apenas em `servidor-gitops`.
  Essa credencial nunca será versionada.
- Rollback ocorre revertendo a alteração da imagem no GitOps; o Argo CD aplica
  novamente a versão anterior.
- O primeiro deploy permanece bloqueado até existirem manifests completos,
  healthchecks, armazenamento persistente, backup e segredos externos.


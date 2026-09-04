# ADR-0031: Persistência, recursos, backup e segredos no K3s

Status: Aceita
Data: 2026-09-04

## Contexto

O primeiro deploy completo do sistema precisa incluir cinco esquemas MySQL,
MongoDB, Kafka, Qdrant, Keycloak e sete aplicações. O servidor é um nó único
K3s com 12 threads, 46 GiB de RAM, 145 GiB livres no sistema e armazenamento
dedicado do Kubernetes em `/mnt/kubernetes`. Alta disponibilidade real não é
possível dentro do mesmo host; por isso, recuperação testada e cópia externa
são mais importantes que réplicas que compartilhariam a mesma falha física.

## Decisão

### Topologia e persistência

- Namespace da aplicação: `financas-producao`, separado de `apps-producao`.
- Os sete componentes de aplicação são `Deployment` stateless e usam imagens
  do GHCR fixadas por SHA.
- MySQL, MongoDB, Kafka em modo KRaft e Qdrant usam `StatefulSet`, uma réplica
  e `PersistentVolumeClaim` no disco dedicado do Kubernetes.
- Será criada uma StorageClass baseada no Local Path Provisioner com
  `reclaimPolicy: Retain`. Exclusão de workload ou PVC não deve apagar o
  diretório de dados automaticamente.
- Um único MySQL mantém bancos e usuários logicamente separados para
  `account_db`, `transaction_db`, `card_db`, `document_db`, `budget_db` e o
  banco do Keycloak. Isso reduz consumo no nó único sem misturar schemas.
- O Keycloak é stateless e persiste no MySQL; o modo de desenvolvimento e o
  banco embutido não são permitidos em produção.
- Redis não entra no primeiro deploy porque nenhuma configuração efetiva dos
  serviços o consome hoje. Ele será incluído apenas quando houver caso real.
- Ollama permanece no container com GPU já operado pelo host. `ai-service` e
  `document-service` o acessam por endereço privado; não haverá GPU pass-through
  para o K3s nesta etapa.

### Capacidade inicial

Os valores são ponto de partida e serão recalibrados com métricas reais. CPU
é compressível; memória recebe limite explícito para proteger o nó.

| Componente | Réplicas | CPU request/limit | Memória request/limit | PVC inicial |
|---|---:|---:|---:|---:|
| `web` | 1 | 100m / 500m | 256Mi / 512Mi | — |
| `account-service` | 1 | 150m / 1 | 384Mi / 768Mi | — |
| `transaction-service` | 1 | 150m / 1 | 384Mi / 768Mi | — |
| `card-service` | 1 | 150m / 1 | 384Mi / 768Mi | — |
| `budget-service` | 1 | 150m / 1 | 384Mi / 768Mi | — |
| `document-service` | 1 | 250m / 1500m | 512Mi / 1Gi | — |
| `ai-service` | 1 | 250m / 1500m | 512Mi / 1Gi | — |
| MySQL 8.4 | 1 | 500m / 2 | 1Gi / 3Gi | 30Gi |
| MongoDB 7 | 1 | 300m / 1500m | 768Mi / 2Gi | 20Gi |
| Kafka KRaft | 1 | 500m / 2 | 1Gi / 3Gi | 20Gi |
| Qdrant | 1 | 300m / 1500m | 512Mi / 2Gi | 20Gi |
| Keycloak | 1 | 300m / 1 | 512Mi / 1536Mi | — |
| Vault | 1 | 100m / 500m | 256Mi / 512Mi | 5Gi |
| External Secrets Operator | 1 | 50m / 300m | 128Mi / 256Mi | — |
| OpenTelemetry Collector | 1 | 100m / 500m | 256Mi / 512Mi | — |

O conjunto solicita aproximadamente 7,1 GiB de RAM e limita o pico nominal em
cerca de 19 GiB, preservando ampla margem para K3s, Argo CD, serviços Docker
existentes, Ollama e cache do sistema.

### Segredos

- HashiCorp Vault, com armazenamento Raft e TLS interno, é a fonte de verdade.
- External Secrets Operator materializa apenas os `Secret` necessários no
  namespace. Manifests GitOps contêm nomes e referências, nunca valores.
- O token de bootstrap, chaves de unseal/recovery e cópia de emergência ficam
  fora do Git, fora do cluster e fora do próprio servidor.
- A autenticação dos workloads usa o método Kubernetes do Vault e políticas
  por aplicação. Um serviço não lê segredo de outro.
- O acesso privado ao GHCR usa um `imagePullSecret` gerenciado pelo mesmo
  mecanismo e uma credencial somente de leitura.
- A implantação deve falhar se um segredo obrigatório estiver ausente; não há
  fallback para senhas de desenvolvimento.

### Backup e recuperação

- MySQL: `mysqldump --single-transaction` diário de todos os bancos do sistema.
- MongoDB: `mongodump` diário com autenticação.
- Qdrant: snapshot diário das collections.
- Vault: snapshot Raft diário, cifrado antes de sair do host.
- Kafka não é fonte de verdade. O PVC permite reinício local, mas mensagens não
  substituem os bancos; a aplicação deve tolerar reprocessamento idempotente.
- Os artefatos são cifrados e enviados pela Tailscale à VPS Oracle, aproveitando
  o destino externo já existente. Retenção: 7 diários, 4 semanais e 6 mensais.
- Uma restauração completa em namespace temporário deve ser testada antes da
  publicação do sistema e trimestralmente depois dela.
- Backup não é considerado válido só porque o arquivo existe: checksum,
  catálogo e restauração amostral fazem parte do job.

### Operação

- Probes de startup, readiness e liveness serão definidas por aplicação.
- `PodDisruptionBudget` não será usado com uma réplica em nó único, pois não
  criaria disponibilidade real e poderia bloquear manutenção.
- NetworkPolicies liberam apenas os fluxos necessários entre web, APIs,
  bancos, Kafka, Keycloak, Qdrant, Vault/ESO e observabilidade.
- Alertas mínimos: PVC acima de 75%, memória acima de 85%, reinícios, probe
  falhando, backup atrasado e certificado próximo do vencimento.

## Consequências

- O primeiro deploy cabe com folga no servidor atual e deixa espaço para os
  demais projetos.
- StatefulSets de uma réplica não protegem contra perda do host; a proteção é
  backup externo testado.
- A StorageClass `Retain` exige limpeza manual consciente de volumes antigos.
- Vault e External Secrets adicionam operação, mas eliminam segredos em Git e
  estabelecem um padrão reutilizável para projetos futuros.
- Crescimento de dados, latência ou necessidade de HA exigirá banco gerenciado,
  outro nó ou migração seletiva de componentes; não será simulada HA no mesmo
  servidor.

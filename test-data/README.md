# test-data

Pasta local pra arquivos de referência/teste com dado real (fatura, extrato,
foto de boleto etc.) usados durante o desenvolvimento do `document-service`.

**Nunca commitada.** Todo o conteúdo dessa pasta (exceto este README) está no
`.gitignore` da raiz — mesmo o repositório sendo privado, dado financeiro
real de terceiros (nome, CPF, valores) não deve entrar no histórico do git.

Senha de PDF protegido (ex: CPF do titular), se precisar pra algum teste,
também não deve ir num arquivo aqui dentro nem em nenhum arquivo versionado —
usar variável de ambiente local (`.env`, já gitignored) na hora de rodar o
teste.

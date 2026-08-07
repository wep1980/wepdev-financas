CREATE TABLE transacoes (
    id                      CHAR(36)       NOT NULL PRIMARY KEY,
    conta_id                CHAR(36)       NOT NULL,
    usuario_id              CHAR(36)       NOT NULL,
    descricao               VARCHAR(255)   NOT NULL,
    valor                   DECIMAL(19,2)  NOT NULL,
    tipo                    VARCHAR(10)    NOT NULL,
    categoria               VARCHAR(100),
    data_transacao          DATE           NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    transacao_recorrente_id CHAR(36),
    criado_em               DATETIME(6)    NOT NULL,

    INDEX idx_transacoes_usuario_id (usuario_id),
    INDEX idx_transacoes_conta_id (conta_id)
);

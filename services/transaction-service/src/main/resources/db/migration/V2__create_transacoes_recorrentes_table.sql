CREATE TABLE transacoes_recorrentes (
    id                      CHAR(36)       NOT NULL PRIMARY KEY,
    conta_id                CHAR(36)       NOT NULL,
    usuario_id              CHAR(36)       NOT NULL,
    descricao               VARCHAR(255)   NOT NULL,
    valor                   DECIMAL(19,2)  NOT NULL,
    tipo                    VARCHAR(10)    NOT NULL,
    categoria               VARCHAR(100),
    frequencia              VARCHAR(10)    NOT NULL,
    data_inicio             DATE           NOT NULL,
    quantidade_ocorrencias  INT,
    ocorrencias_geradas     INT            NOT NULL,
    status                  VARCHAR(20)    NOT NULL,
    criado_em               DATETIME(6)    NOT NULL,

    INDEX idx_transacoes_recorrentes_usuario_id (usuario_id),
    INDEX idx_transacoes_recorrentes_status (status)
);

CREATE TABLE lancamentos_pendentes (
    id                    CHAR(36)       NOT NULL PRIMARY KEY,
    documento_id          CHAR(36)       NOT NULL,
    descricao             VARCHAR(255)   NOT NULL,
    valor                 DECIMAL(19,2)  NOT NULL,
    data                  DATE           NOT NULL,
    tipo                  VARCHAR(10)    NOT NULL,
    categoria_sugerida    VARCHAR(100),
    status                VARCHAR(10)    NOT NULL,

    INDEX idx_lancamentos_pendentes_documento_id (documento_id)
);

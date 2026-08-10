CREATE TABLE orcamentos (
    id                CHAR(36)       NOT NULL PRIMARY KEY,
    usuario_id        CHAR(36)       NOT NULL,
    categoria         VARCHAR(255)   NOT NULL,
    mes_referencia    CHAR(7)        NOT NULL COMMENT 'formato AAAA-MM',
    valor_limite      DECIMAL(19,2)  NOT NULL,
    status            VARCHAR(10)    NOT NULL,
    criado_em         DATETIME(6)    NOT NULL,

    INDEX idx_orcamentos_usuario_mes (usuario_id, mes_referencia)
);

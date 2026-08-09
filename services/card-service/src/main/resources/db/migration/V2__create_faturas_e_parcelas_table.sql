CREATE TABLE faturas (
    id                CHAR(36)       NOT NULL PRIMARY KEY,
    cartao_id         CHAR(36)       NOT NULL,
    usuario_id        CHAR(36)       NOT NULL,
    competencia       CHAR(7)        NOT NULL COMMENT 'formato AAAA-MM',
    data_fechamento   DATE           NOT NULL,
    data_vencimento   DATE           NOT NULL,
    valor_total       DECIMAL(19,2)  NOT NULL,
    status            VARCHAR(10)    NOT NULL,

    UNIQUE KEY uk_faturas_cartao_competencia (cartao_id, competencia),
    INDEX idx_faturas_usuario_id (usuario_id),
    INDEX idx_faturas_status (status)
);

CREATE TABLE parcelas (
    id                    CHAR(36)       NOT NULL PRIMARY KEY,
    fatura_id             CHAR(36)       NOT NULL,
    compra_id             CHAR(36)       NOT NULL,
    descricao             VARCHAR(255)   NOT NULL,
    valor                 DECIMAL(19,2)  NOT NULL,
    categoria             VARCHAR(100),
    numero_parcela        INT            NOT NULL,
    quantidade_parcelas   INT            NOT NULL,

    INDEX idx_parcelas_fatura_id (fatura_id),
    INDEX idx_parcelas_compra_id (compra_id)
);

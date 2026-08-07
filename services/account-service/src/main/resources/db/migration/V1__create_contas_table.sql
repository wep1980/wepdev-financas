CREATE TABLE contas (
    id           CHAR(36)       NOT NULL PRIMARY KEY,
    usuario_id   CHAR(36)       NOT NULL,
    nome         VARCHAR(255)   NOT NULL,
    tipo         VARCHAR(20)    NOT NULL,
    saldo        DECIMAL(19,2)  NOT NULL,
    instituicao  VARCHAR(255),
    ativa        BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em    DATETIME(6)    NOT NULL,
    atualizado_em DATETIME(6)   NOT NULL,

    INDEX idx_contas_usuario_id (usuario_id)
);

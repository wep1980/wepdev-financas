CREATE TABLE cartoes (
    id                    CHAR(36)       NOT NULL PRIMARY KEY,
    usuario_id            CHAR(36)       NOT NULL,
    apelido               VARCHAR(255)   NOT NULL,
    bandeira              VARCHAR(20),
    limite                DECIMAL(19,2)  NOT NULL,
    dia_fechamento        TINYINT        NOT NULL,
    dia_vencimento        TINYINT        NOT NULL,
    conta_pagamento_id    CHAR(36)       NOT NULL,
    ativo                 BOOLEAN        NOT NULL,
    criado_em             DATETIME(6)    NOT NULL,

    INDEX idx_cartoes_usuario_id (usuario_id)
);

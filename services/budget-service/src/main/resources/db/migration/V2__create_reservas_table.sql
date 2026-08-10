CREATE TABLE reservas (
    usuario_id      CHAR(36)       NOT NULL PRIMARY KEY,
    valor           DECIMAL(19,2)  NOT NULL,
    atualizado_em   DATETIME(6)    NOT NULL
);

ALTER TABLE lancamentos_pendentes
    ADD COLUMN numero_parcela      INT NOT NULL DEFAULT 1,
    ADD COLUMN quantidade_parcelas INT NOT NULL DEFAULT 1;

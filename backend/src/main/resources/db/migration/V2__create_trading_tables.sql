CREATE TABLE holdings (
    id                 BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            BIGINT         NOT NULL,
    ticker             VARCHAR(10)    NOT NULL,
    quantity           INT            NOT NULL,
    average_cost_basis NUMERIC(14, 2) NOT NULL,
    updated_at         TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_holdings FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_holdings_user_ticker UNIQUE (user_id, ticker)
);

CREATE TABLE transactions (
    id              BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    ticker          VARCHAR(10)    NOT NULL,
    side            VARCHAR(4)     NOT NULL CHECK (side IN ('BUY', 'SELL')),
    quantity        INT            NOT NULL,
    price_per_share NUMERIC(14, 2) NOT NULL,
    total_amount    NUMERIC(14, 2) NOT NULL,
    executed_at     TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_transactions FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_transactions_user_executed_at ON transactions (user_id, executed_at);

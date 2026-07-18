CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255)   NOT NULL UNIQUE,
    username        VARCHAR(50)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)   NOT NULL,
    cash_balance    NUMERIC(14, 2) NOT NULL DEFAULT 500.00,
    failed_attempts INT            NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

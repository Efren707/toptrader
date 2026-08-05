CREATE TABLE password_reset_tokens (
    id              BIGINT         GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT         NOT NULL,
    token_hash      VARCHAR(64)    NOT NULL,
    expires_at      TIMESTAMP      NOT NULL,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_password_reset_token FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_created_at ON password_reset_tokens (user_id, created_at);